import type { InnblikkBeforeSend } from "./innblikk";

export {};

declare global {
  interface Window {
    innblikkBeforeSend?: InnblikkBeforeSend;
    umami?: {
      track: (
        event: string | ((payload: Record<string, unknown>) => Record<string, unknown>),
        data?: Record<string, unknown>,
      ) => void;
    };
  }
}
