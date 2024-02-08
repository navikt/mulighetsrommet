/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_MULIGHETSROMMET_API_AUTH_TOKEN?: string;
  readonly VITE_MULIGHETSROMMET_API_BASE?: string;
  readonly VITE_ENVIRONMENT?: environments;
  readonly VITE_MULIGHETSROMMET_API_MOCK?: "true" | "false";
  readonly VITE_EKSTERNE_SYSTEMER_MOCK?: "true" | "false";
  readonly VITE_SANITY_PROJECT_ID?: string;
  readonly VITE_SANITY_DATASET?: string;
  readonly VITE_SANITY_ACCESS_TOKEN?: string;
  readonly VITE_FARO_URL?: string;
}

export type environments = "localhost" | "dev" | "production";

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

export type LogEventFromApp = (params?: {
  origin: unknown | string;
  eventName: unknown | string;
  eventData?: unknown | Record<string, unknown>;
}) => Promise<void>;

declare global {
  interface Window {
    veilarbpersonflatefsAmplitude: LogEventFromApp;
  }
}
