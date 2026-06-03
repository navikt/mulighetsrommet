/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_MULIGHETSROMMET_API_AUTH_TOKEN?: string;
  readonly VITE_MULIGHETSROMMET_API_BASE?: string;
  readonly VITE_MULIGHETSROMMET_API_MOCK?: "true" | "false";
  readonly VITE_BASE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
