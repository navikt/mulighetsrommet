import { createProxyMiddleware } from "http-proxy-middleware";
import { getToken, requestTokenxOboToken, validateToken } from "@navikt/oasis";
import { v4 as uuidv4 } from "uuid";
import logger from "./logger.js";

const apiBaseUrl = process.env.VITE_MULIGHETSROMMET_API_BASE || "http://mulighetsrommet-api";

function isLocalOrDemo() {
  return !process.env.NAIS_CLUSTER_NAME || process.env.VITE_MULIGHETSROMMET_API_MOCK === "true";
}

function authFailure(reason, status, details) {
  return { ok: false, reason, status, details };
}

export async function isRequestAuthenticated(req) {
  if (isLocalOrDemo()) {
    return true;
  }

  const token = getToken(req);
  if (!token) {
    return false;
  }

  const validation = await validateToken(token);
  return validation.ok;
}

async function getApiToken(req) {
  if (isLocalOrDemo()) {
    // In local/demo mode, return a dummy token or the configured one
    // If no token is configured, return a placeholder to allow mocked requests
    return {
      ok: true,
      token: process.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN || "local-dev-token",
    };
  }

  const token = getToken(req);

  if (!token) {
    return authFailure("missing_token", 401);
  }

  const validation = await validateToken(token);
  if (!validation.ok) {
    return authFailure("invalid_token", 401, validation.errorType);
  }

  const audience = `${process.env.NAIS_CLUSTER_NAME}:team-mulighetsrommet:mulighetsrommet-api`;
  const obo = await requestTokenxOboToken(token, audience);
  if (!obo.ok) {
    return authFailure("obo_exchange_failed", 502, obo.error?.message);
  }

  return { ok: true, token: obo.token };
}

const proxy = createProxyMiddleware({
  target: apiBaseUrl,
  changeOrigin: true,
  pathRewrite: {
    "^/api-proxy": "",
  },
  on: {
    proxyReq: (proxyReq, req) => {
      if (req.apiToken) {
        proxyReq.setHeader("Authorization", `Bearer ${req.apiToken}`);
      }
      proxyReq.setHeader("Accept", "application/json");
      proxyReq.setHeader("Nav-Consumer-Id", uuidv4());
    },
    error: (err, _req, res) => {
      logger.error("API proxy error:", err);
      if (res.writeHead) {
        res.writeHead(500, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "Proxy error" }));
      }
    },
  },
});

export async function apiProxy(req, res, next) {
  try {
    const tokenResult = await getApiToken(req);
    if (!tokenResult.ok) {
      logger.error("API proxy: token acquisition failed", {
        reason: tokenResult.reason,
        status: tokenResult.status,
        details: tokenResult.details,
      });
      return res.status(tokenResult.status).json({ error: "Unauthorized" });
    }
    req.apiToken = tokenResult.token;
    return proxy(req, res, next);
  } catch (error) {
    logger.error("API proxy: error getting token", error);
    return res.status(500).json({ error: "Internal server error" });
  }
}
