import winston from "winston";

const levels = {
  error: 0,
  warn: 1,
  info: 2,
  http: 3,
  debug: 4,
};

const format = winston.format.combine(
  winston.format.timestamp(),
  winston.format.errors({ stack: true }),
  winston.format.metadata({ fillWith: ["timestamp", "service", "env"] }),
  winston.format.json(),
);

const transports = [new winston.transports.Console()];

const logger = winston.createLogger({
  levels,
  format,
  defaultMeta: {
    service: "arrangor-flate",
    // eslint-disable-next-line no-undef
    env: process.env.NODE_ENV || "development",
  },
  transports,
  // Enable exception handling
  handleExceptions: true,
  handleRejections: true,
});

export default logger;
