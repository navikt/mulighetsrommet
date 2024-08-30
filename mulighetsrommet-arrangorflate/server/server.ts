import compression from "compression";
import type { Response } from "express";
import express from "express";
import { logger } from "./logger";
import handleRequestWithRemix from "./remix";

const port = process.env.PORT || 3000;
const basePath = "/kandidatliste";

const app = express();

const configureServerSettings = () => {
  // Gzip responser
  app.use(compression());

  // Sikkerhetsgreier
  app.disable("x-powered-by");

  // Remix bygger appen med fingerprinting, så vi kan cache disse filene evig
  app.use(`${basePath}/build`, express.static("public/build", { immutable: true, maxAge: "1y" }));

  // Cache public-filer (som favicon) i én time
  app.use(express.static("public", { maxAge: "1h" }));
};

const startServer = async () => {
  configureServerSettings();

  app.get([`${basePath}/internal/isAlive`, `${basePath}/internal/isReady`], (_, res: Response) =>
    res.sendStatus(200),
  );

  app.all("*", handleRequestWithRemix);

  app.listen(port, () => {
    logger.info(`Server kjører på port ${port}`);
  });
};

startServer();
