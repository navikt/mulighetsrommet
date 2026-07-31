/// <reference types="vite/client" />

// Pre-built worker artifact shipped without TypeScript declarations.
declare module "pdfjs-dist/build/pdf.worker.min.mjs" {}

interface ImportMetaEnv {
  readonly VITE_MULIGHETSROMMET_API_AUTH_TOKEN?: string;
  readonly VITE_MULIGHETSROMMET_API_BASE?: string;
  readonly VITE_MULIGHETSROMMET_API_MOCK?: "true" | "false";
  readonly VITE_BASE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
