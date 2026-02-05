import { createProxyMiddleware } from "http-proxy-middleware";
import { getToken, requestTokenxOboToken, validateToken } from "@navikt/oasis";
import { v4 as uuidv4 } from "uuid";
import logger from "./logger.js";

const apiBaseUrl = process.env.VITE_MULIGHETSROMMET_API_BASE || "http://mulighetsrommet-api";
const isLocalOrDemo =
  process.env.NAIS_CLUSTER_NAME === undefined ||
  process.env.VITE_MULIGHETSROMMET_API_MOCK === "true";

async function getApiToken(req) {
  if (isLocalOrDemo) {
    // In local/demo mode, return a dummy token or the configured one
    // If no token is configured, return a placeholder to allow mocked requests
    return process.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN || "local-dev-token";
  }

  const token = getToken(req);

  if (!token) {
    logger.error("API proxy: missing token");
    return null;
  }

  const validation = await validateToken(token);
  if (!validation.ok) {
    logger.error("API proxy: invalid token");
    return null;
  }

  const audience = `${process.env.NAIS_CLUSTER_NAME}:team-mulighetsrommet:mulighetsrommet-api`;
  const obo = await requestTokenxOboToken(token, audience);
  if (!obo.ok) {
    logger.error("API proxy: OBO exchange failed", obo);
    return null;
  }

  return obo.token;
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
    const token = await getApiToken(req);
    if (!token) {
      logger.error("API proxy: no token available");
      return res.status(401).json({ error: "Unauthorized" });
    }
    req.apiToken = token;
    return proxy(req, res, next);
  } catch (error) {
    logger.error("API proxy: error getting token", error);
    return res.status(500).json({ error: "Internal server error" });
  }
}
