import type { RequestHandler } from "express";
import winston from "winston";

export const logger = winston.createLogger({
    format:
        process.env.NODE_ENV === "development" ? winston.format.simple() : winston.format.json(),
    transports: new winston.transports.Console(),
});

export const logRequests: RequestHandler = (request, res, next) => {
    const method = request.method;
    const url = request.url;

    logger.info(`${method} ${url}`);

    next();
};
