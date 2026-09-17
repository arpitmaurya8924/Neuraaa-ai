/**
 * NEURA Secure Backend API Server
 *
 * Architecture:
 * NEURA App
 *    ↓
 * NEURA Backend API (This Service)
 *    ↓
 * Gemini API (Google Generative AI) — tries PRIMARY model, then FALLBACK models
 *    ↓
 * Backend receives response
 *    ↓
 * NEURA App displays response
 *
 * Security:
 * - Reads GEMINI_API_KEY exclusively from process.env.GEMINI_API_KEY
 * - Never returns or exposes the API key to the client
 * - Proxies requests to Google Gemini REST API with SSE streaming support
 *
 * CHANGES vs previous version:
 * - Added real multi-model failover (previously there was only ONE model call,
 *   so any single Gemini error surfaced straight to the app as a hard failure).
 * - Error messages now say EXACTLY why a call failed (bad/missing key, quota
 *   exceeded, invalid/retired model name, or Gemini being overloaded) instead
 *   of a generic "provider failed" message, so the real cause is visible in
 *   Render logs and in the app.
 */

import http from 'node:http';
import { URL } from 'node:url';

const PORT = process.env.PORT || 3001;

// Ordered list of models to try. First is primary, rest are fallbacks tried
// in order if the previous one fails. Override the primary with the
// GEMINI_MODEL env var if you want to pin a specific model.
const MODEL_CHAIN = [
  process.env.GEMINI_MODEL || 'gemini-3.5-flash',
  'gemini-3.6-flash',
  'gemini-3.5-flash-lite'
].filter((v, i, arr) => arr.indexOf(v) === i); // de-dupe

// Gemini API base URL
const GEMINI_BASE_URL = 'https://generativelanguage.googleapis.com/v1beta';

function getApiKey() {
  return process.env.GEMINI_API_KEY || '';
}

function setCorsHeaders(res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
}

// Turns a Gemini HTTP failure into a clear, specific reason string.
function describeGeminiFailure(status, rawBody) {
  let message = rawBody;
  try {
    const parsed = JSON.parse(rawBody);
    message = parsed.error?.message || rawBody;
  } catch (_) {
    // rawBody wasn't JSON, use as-is
  }

  if (status === 400) return `Bad request (model or payload issue): ${message}`;
  if (status === 401 || status === 403) {
    return `GEMINI_API_KEY is invalid, restricted, or missing required permissions: ${message}`;
  }
  if (status === 404) return `Model not found (name may be retired/typo'd): ${message}`;
  if (status === 429) return `Gemini quota/rate limit exceeded: ${message}`;
  if (status === 500 || status === 503) return `Gemini is temporarily overloaded: ${message}`;
  return `Gemini API error (${status}): ${message}`;
}

async function callGeminiNonStreaming(model, apiKey, geminiRequestBody) {
  const url = `${GEMINI_BASE_URL}/models/${model}:generateContent?key=${apiKey}`;
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(geminiRequestBody)
  });
  const bodyText = await res.text();
  if (!res.ok) {
    const reason = describeGeminiFailure(res.status, bodyText);
    return { ok: false, status: res.status, reason };
  }
  let data;
  try {
    data = JSON.parse(bodyText);
  } catch (_) {
    return { ok: false, status: 500, reason: 'Gemini returned a non-JSON response.' };
  }
  const responseText = data.candidates?.[0]?.content?.parts?.[0]?.text || '';
  return { ok: true, model, responseText };
}

const server = http.createServer(async (req, res) => {
  setCorsHeaders(res);

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
  const pathname = parsedUrl.pathname;

  // 1. Healthcheck Endpoint
  if (req.method === 'GET' && (pathname === '/api/health' || pathname === '/health' || pathname === '/')) {
    const apiKeyConfigured = Boolean(getApiKey() && getApiKey() !== 'MY_GEMINI_API_KEY');
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      service: 'NEURA AI Backend API',
      status: 'healthy',
      modelChain: MODEL_CHAIN,
      apiKeyConfigured,
      environment: process.env.NODE_ENV || 'production'
    }));
    return;
  }

  // 2. Chat Streaming Endpoint (SSE) + non-streaming /api/chat
  if (req.method === 'POST' && (pathname === '/api/chat/stream' || pathname === '/api/chat')) {
    const apiKey = getApiKey();
    if (!apiKey || apiKey === 'MY_GEMINI_API_KEY') {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        error: 'Unauthorized: GEMINI_API_KEY is not configured on the backend server.',
        hint: 'Set GEMINI_API_KEY in the server environment variables or in AI Studio Secrets panel.'
      }));
      return;
    }

    let bodyRaw = '';
    req.on('data', chunk => { bodyRaw += chunk; });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(bodyRaw || '{}');
        const {
          prompt,
          conversationHistory = [],
          imageBase64 = null,
          systemInstruction = 'You are NEURA, an intelligent AI companion created by Arpit.',
          mode = 'general',
          maxTokens = 2000
        } = payload;

        if (!prompt && !imageBase64) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Prompt or image attachment is required.' }));
          return;
        }

        // Build Gemini contents array
        const contents = [];
        for (const msg of conversationHistory) {
          const role = (msg.role === 'assistant' || msg.role === 'model') ? 'model' : 'user';
          contents.push({
            role,
            parts: [{ text: msg.content || '' }]
          });
        }

        const currentParts = [];
        if (prompt) currentParts.push({ text: prompt });
        if (imageBase64) {
          currentParts.push({
            inlineData: { mimeType: 'image/jpeg', data: imageBase64 }
          });
        }
        contents.push({ role: 'user', parts: currentParts });

        const geminiRequestBody = {
          contents,
          systemInstruction: { parts: [{ text: systemInstruction }] },
          generationConfig: {
            temperature: 0.7,
            topP: 0.95,
            maxOutputTokens: maxTokens
          }
        };

        const isStreaming = pathname.includes('/stream') || req.headers.accept?.includes('text/event-stream');

        if (isStreaming) {
          // SSE Streaming Response — try each model in MODEL_CHAIN until one
          // starts streaming successfully. Once a stream has started, we
          // relay it as-is (no further failover mid-stream).
          res.writeHead(200, {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive'
          });

          const failures = [];
          let streamed = false;

          for (const model of MODEL_CHAIN) {
            const geminiStreamUrl = `${GEMINI_BASE_URL}/models/${model}:streamGenerateContent?alt=sse&key=${apiKey}`;
            try {
              const geminiRes = await fetch(geminiStreamUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(geminiRequestBody)
              });

              if (!geminiRes.ok) {
                const errBody = await geminiRes.text();
                failures.push(`[${model}] ${describeGeminiFailure(geminiRes.status, errBody)}`);
                continue; // try next model in the chain
              }

              // Success: relay the stream through and stop trying further models.
              streamed = true;
              const reader = geminiRes.body.getReader();
              const decoder = new TextDecoder();
              while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                res.write(decoder.decode(value, { stream: true }));
              }
              break;
            } catch (streamErr) {
              failures.push(`[${model}] Network/connection error: ${streamErr.message}`);
            }
          }

          if (!streamed) {
            const detail = failures.join(' | ');
            res.write(`data: ${JSON.stringify({
              error: `All Gemini models failed. ${detail}`
            })}\n\n`);
          }
          res.write('data: [DONE]\n\n');
          res.end();
        } else {
          // Standard Non-Streaming Response with failover across MODEL_CHAIN
          const failures = [];
          let success = null;

          for (const model of MODEL_CHAIN) {
            const result = await callGeminiNonStreaming(model, apiKey, geminiRequestBody);
            if (result.ok) {
              success = result;
              break;
            }
            failures.push(`[${model}] ${result.reason}`);
          }

          if (success) {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ response: success.responseText, model: success.model }));
          } else {
            res.writeHead(503, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
              error: `All Gemini models failed. ${failures.join(' | ')}`
            }));
          }
        }
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Internal Server Error', message: err.message }));
      }
    });
    return;
  }

  // Not Found
  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Endpoint not found' }));
});

server.listen(PORT, () => {
  console.log(`[NEURA] Secure AI Backend Server running on port ${PORT}`);
  console.log(`[NEURA] Gemini model chain: ${MODEL_CHAIN.join(' -> ')}`);
  console.log(`[NEURA] Gemini API Key configured: ${Boolean(getApiKey())}`);
});
