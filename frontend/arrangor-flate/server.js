import { createRequestHandler } from "@remix-run/express";
import compression from "compression";
import { createProxyMiddleware } from 'http-proxy-middleware';
import express from "express";
import morgan from "morgan";
import { getToken, requestTokenxOboToken, validateToken } from '@navikt/oasis';
import expressPromBundle from "express-prom-bundle";
const metricsMiddleware = expressPromBundle({ includeMethod: true, includePath: true });

const port = process.env.PORT || 3000;
const basePath = "";

const viteDevServer =
  process.env.NODE_ENV === "production"
    ? undefined
    : await import("vite").then((vite) =>
        vite.createServer({
          server: { middlewareMode: true },
        }),
      );

const remixHandler = createRequestHandler({
  build: viteDevServer
    ? () => viteDevServer.ssrLoadModule("virtual:remix/server-build")
    : await import("./build/server/index.js"),
  getLoadContext: (req) => ({
    erAutorisert: req.headers.authorization,
  }),
});

const app = express();

app.use(compression());

// http://expressjs.com/en/advanced/best-practice-security.html#at-a-minimum-disable-x-powered-by-header
app.disable("x-powered-by");

// handle asset requests
if (viteDevServer) {
  app.use(viteDevServer.middlewares);
} else {
  // Vite fingerprints its assets so we can cache forever.
  app.use("/assets", express.static("build/client/assets", { immutable: true, maxAge: "1y" }));
}

// Everything else (like favicon.ico) is cached for an hour. You may want to be
// more aggressive with this caching.
app.use(express.static("build/client", { maxAge: "1h" }));

app.get([`${basePath}/internal/isAlive`, `${basePath}/internal/isReady`], (_, res) =>
  res.sendStatus(200),
);

app.use(metricsMiddleware);
app.use(morgan("tiny"));

if (process.env.VITE_MULIGHETSROMMET_API_MOCK !== 'true') {
  app.use(
    "/api",
    oboMiddleware(),
    proxyMiddleware(),
  );
}

// handle SSR requests
app.all("*", remixHandler);

app.listen(port, () => {
  const env = process.env.NODE_ENV || "development";
  // eslint-disable-next-line no-console
  console.log(
    env === "development"
      ? `Server kjører på http://localhost:${port}`
      : `Serveren kjører på port ${port}`,
  );
});

function oboMiddleware() {
  return  async (req, res, next) => {
    if (process.env.NODE_ENV !== 'production') {
      return next();
    }
    const token = getToken(req);
    if (!token) {
      console.log("token missing", token);
      return res.redirect("/oath2/login");
    }
    const validation = await validateToken(token);
    if (!validation.ok) {
      console.log("token validation failed", token);
      return res.redirect("/oath2/login");
    }
    // TODO: Tror man skal kanskje heller bruke den generiske requestObboToken i stedet
    const obo = await requestTokenxOboToken(token,  `${process.env.NAIS_CLUSTER_NAME}.team-mulighetsrommet.mulighetsrommet-api`);
    if (obo.ok) {
      console.log("OBO-exchange success");
      req.headers['obo-token'] = obo.token;
      return next();
    } else {
      console.log('OBO-exchange failed', obo.error);
      return res.status(403).send();
    }
  }
}

function proxyMiddleware() {
  return  createProxyMiddleware({
    target: process.env.VITE_MULIGHETSROMMET_API_BASE,
    changeOrigin: true,
		pathRewrite: { '^/': '/api/' },
    logger: console,
    on: {
      proxyReq: (proxyRequest, request) => {
        const obo = request.headers['obo-token'];
        if (obo) {
          proxyRequest.removeHeader('obo-token');
          proxyRequest.removeHeader('cookie');
          proxyRequest.setHeader('Authorization', `Bearer ${obo}`);
        } else {
          console.log("Access token var not present in session");
        }
      },
    },
  });
}
