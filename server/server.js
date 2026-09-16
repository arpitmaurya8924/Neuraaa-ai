/**
 * NEURA Secure Backend API Server
 * 
 * Architecture:
 * NEURA App
 *    ↓
 * NEURA Backend API (This Service)
 *    ↓
 * Gemini API (Google Generative AI)
 *    ↓
 * Backend receives response
 *    ↓
 * NEURA App displays response
 *
 * Security:
 * - Reads GEMINI_API_KEY exclusively from process.env.GEMINI_API_KEY
 * - Never returns or exposes the API key to the client
 * - Proxies requests to Google Gemini REST API with SSE streaming support
 */

import http from 'node:http';
import { URL } from 'node:url';

const PORT = process.env.PORT || 3001;
const DEFAULT_MODEL = process.env.GEMINI_MODEL || 'gemini-3.8-flash';

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
      model: DEFAULT_MODEL,
      apiKeyConfigured,
      environment: process.env.NODE_ENV || 'production'
    }));
    return;
  }

  // 2. Chat Streaming Endpoint (SSE)
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
        if (prompt) {
          currentParts.push({ text: prompt });
        }
        if (imageBase64) {
          currentParts.push({
            inlineData: {
              mimeType: 'image/jpeg',
              data: imageBase64
            }
          });
        }
        contents.push({ role: 'user', parts: currentParts });

        const geminiRequestBody = {
          contents,
          systemInstruction: {
            parts: [{ text: systemInstruction }]
          },
          generationConfig: {
            temperature: 0.7,
            topP: 0.95,
            maxOutputTokens: maxTokens
          }
        };

        const isStreaming = pathname.includes('/stream') || req.headers.accept?.includes('text/event-stream');

        if (isStreaming) {
          // SSE Streaming Response
          res.writeHead(200, {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive'
          });

          const geminiStreamUrl = `${GEMINI_BASE_URL}/models/${DEFAULT_MODEL}:streamGenerateContent?alt=sse&key=${apiKey}`;
          
          try {
            const geminiRes = await fetch(geminiStreamUrl, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(geminiRequestBody)
            });

            if (!geminiRes.ok) {
              const errBody = await geminiRes.text();
              res.write(`data: ${JSON.stringify({ error: `Gemini API error (${geminiRes.status}): ${errBody}` })}\n\n`);
              res.write('data: [DONE]\n\n');
              res.end();
              return;
            }

            const reader = geminiRes.body.getReader();
            const decoder = new TextDecoder();

            while (true) {
              const { done, value } = await reader.read();
              if (done) break;
              const textChunk = decoder.decode(value, { stream: true });
              res.write(textChunk);
            }

            res.write('data: [DONE]\n\n');
            res.end();
          } catch (streamErr) {
            res.write(`data: ${JSON.stringify({ error: streamErr.message })}\n\n`);
            res.write('data: [DONE]\n\n');
            res.end();
          }
        } else {
          // Standard Non-Streaming Response
          const geminiUrl = `${GEMINI_BASE_URL}/models/${DEFAULT_MODEL}:generateContent?key=${apiKey}`;
          const geminiRes = await fetch(geminiUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(geminiRequestBody)
          });

          const data = await geminiRes.json();
          if (!geminiRes.ok) {
            res.writeHead(geminiRes.status, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: data.error?.message || 'Gemini API call failed' }));
            return;
          }

          const responseText = data.candidates?.[0]?.content?.parts?.[0]?.text || '';
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            response: responseText,
            model: DEFAULT_MODEL
          }));
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
  console.log(`[NEURA] Gemini model: ${DEFAULT_MODEL}`);
  console.log(`[NEURA] Gemini API Key configured: ${Boolean(getApiKey())}`);
});
