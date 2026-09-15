# NEURA AI — Backend & Gemini API Security Configuration

## Architecture Overview

NEURA is built with a decoupled, secure client-to-backend architecture:

```
┌─────────────────────────┐
│     NEURA Android App   │
│  (Chat UI & ViewModels) │
└────────────┬────────────┘
             │  (Clean JSON / SSE Streaming)
             ▼
┌─────────────────────────┐
│   NEURA Backend API     │
│ (NeuraBackendService /  │
│  Node.js Backend Proxy) │
└────────────┬────────────┘
             │  (Reads GEMINI_API_KEY from server environment)
             ▼
┌─────────────────────────┐
│   Google Gemini API     │
│ (gemini-3.5-flash etc.) │
└────────────┬────────────┘
             │  (Live tokens & candidates)
             ▼
┌─────────────────────────┐
│    NEURA Backend API    │
└────────────┬────────────┘
             │  (Parsed Markdown stream)
             ▼
┌─────────────────────────┐
│   NEURA Android App     │
│    (Renders live)       │
└─────────────────────────┘
```

---

## 🔒 Security Requirements & Guarantees

1. **Zero Client-Side Exposure**:
   The Gemini API key is **never** hardcoded into Android Kotlin source code, layout files, or client-side assets.
2. **Environment Variable Storage**:
   The API key is stored securely as a server-side secret named `GEMINI_API_KEY`.
3. **No Key in Responses**:
   The backend API never exposes or returns the `GEMINI_API_KEY` to the client device.
4. **Resilience & Rate-Limiting**:
   - HTTP 429 rate limit backoff
   - HTTP 500/503 automatic failover to `gemini-3.6-flash`
   - Network connectivity diagnostics

---

## 🛠️ Where to Configure the Secret

### 1. In Google AI Studio (Development & Web Emulator)
1. Open the project in **Google AI Studio**.
2. In the left or right sidebar, navigate to the **Secrets** (Key icon) panel.
3. Add a new secret:
   - **Key**: `GEMINI_API_KEY`
   - **Value**: Your Google Gemini API Key from [Google AI Studio](https://aistudio.google.com/app/apikey).
4. The platform automatically injects this secret at build and container runtime.

### 2. In Production Server / Cloud Deployments (Node.js Backend Proxy)
If deploying the included `server/server.js` standalone proxy to Cloud Run, Render, Railway, or Docker:
```bash
# Set your environment variable
export GEMINI_API_KEY="AIzaSyYourSecretKeyHere"

# Start the server
cd server
npm start
```

---

## 🧩 Modular Model Switching

Model selection is centrally managed in `com.example.data.backend.GeminiModelConfig`:
- **Default Primary**: `gemini-3.5-flash`
- **Fallback**: `gemini-3.6-flash`
- **Deep STEM / Coding**: `gemini-3.1-pro-preview`
- **Vision / Images**: `gemini-2.5-flash-image` / `gemini-3.5-flash`

To change the active model globally across the app, simply update `GeminiModelConfig.primaryModel`.
