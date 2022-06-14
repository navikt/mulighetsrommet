/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_MULIGHETSROMMET_API_AUTH_TOKEN?: string;
  readonly VITE_MULIGHETSROMMET_API_BASE?: string;
  readonly VITE_MULIGHETSROMMET_API_MOCK?: 'true' | 'false';
  readonly VITE_SANITY_PROJECT_ID?: string;
  readonly VITE_SANITY_DATASET?: string;
  readonly VITE_SANITY_ACCESS_TOKEN?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
