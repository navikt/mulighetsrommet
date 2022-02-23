import {
  createFrontendLogger,
  createMockFrontendLogger,
  DEFAULT_FRONTENDLOGGER_API_URL,
} from '@navikt/frontendlogger/lib';
import { APP_NAME } from '../utils/constants';

export const logger = process.env.REACT_APP_DEV
  ? createMockFrontendLogger(APP_NAME)
  : createFrontendLogger(APP_NAME, DEFAULT_FRONTENDLOGGER_API_URL);

export const logError = (fields?: {}, tags?: {}): void => {
  logger.event(`${APP_NAME}.error`, fields, tags);
};

export const logMetrikk = (metrikkNavn: string, fields?: {}, tags?: {}): void => {
  logger.event(`${APP_NAME}.metrikker.${metrikkNavn}`, fields, tags);
};

export const logEvent = (logTag: string, fields?: {}, tags?: {}): void => {
  const frontendlogger = (window as any).frontendlogger;

  if (process.env.REACT_APP_ENABLE_MOCK) {
    console.log('Event', logTag, 'Fields:', fields, 'Tags:', tags); // tslint:disable-line
  } else if (frontendlogger.event) {
    frontendlogger.event(logTag, fields || {}, tags || {});
  }
};
