import {
  createFrontendLogger,
  createMockFrontendLogger,
  DEFAULT_FRONTENDLOGGER_API_URL,
} from '@navikt/frontendlogger/lib';
import { LOG_NAME } from '../constants';

export const logger = import.meta.env.VITE_DEV
  ? createMockFrontendLogger(LOG_NAME)
  : createFrontendLogger(LOG_NAME, DEFAULT_FRONTENDLOGGER_API_URL);

export const logError = (fields?: {}, tags?: {}): void => {
  logger.event(`${LOG_NAME}.error`, fields, tags);
};

export const logMetrikk = (metrikkNavn: string, fields?: {}, tags?: {}): void => {
  logger.event(`${LOG_NAME}.metrikker.${metrikkNavn}`, fields, tags);
};

export const logEvent = (logTag: string, fields?: {}, tags?: {}): void => {
  const frontendlogger = (window as any).frontendlogger;

  if (import.meta.env.VITE_ENABLE_MOCK) {
    console.log('Event', logTag, 'Fields:', fields, 'Tags:', tags); // tslint:disable-line
  } else if (frontendlogger.event) {
    frontendlogger.event(logTag, fields || {}, tags || {});
  }
};
