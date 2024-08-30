import type { RequestHandler } from "express";
import { createProxyMiddleware } from "http-proxy-middleware";
import { logger } from "./logger";

export const setupProxy = (fraPath: string, tilTarget?: string): RequestHandler =>
    createProxyMiddleware(fraPath, {
        target: tilTarget || fraPath,
        changeOrigin: true,
        secure: true,
        pathRewrite: (path) => path.replace(fraPath, ""),
        logProvider: () => logger,
    });
