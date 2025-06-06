import { createRequestHandler } from "@react-router/express";
import compression from "compression";
import express from "express";
import expressPromBundle from "express-prom-bundle";
import morgan from "morgan";
import logger from "./logger.js";

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
    ? () => viteDevServer.ssrLoadModule("virtual:react-router/server-build")
    : await import("../build/server/index.js"),
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

// Create custom morgan stream that writes to Winston
const morganStream = {
  write: (message) => logger.http(message.trim()),
};
app.use(morgan("combined", { stream: morganStream }));

// handle SSR requests
app.all("*splat", remixHandler);

app.listen(port, () => {
  const env = process.env.NODE_ENV || "development";
  logger.info(
    env === "development"
      ? `Server kjører på http://localhost:${port}`
      : `Serveren kjører på port ${port}`,
  );
});
