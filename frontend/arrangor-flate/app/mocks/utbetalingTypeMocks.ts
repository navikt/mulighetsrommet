import { UtbetalingType } from "@api-client";

export const utbetalingType: Record<"KORRIGERING" | "INVESTERING" | "INNSENDING", UtbetalingType> =
  {
    KORRIGERING: {
      type: "KORRIGERING",
      displayName: "Korrigering",
      displayNameLong: null,
      tagName: "KOR",
    },
    INVESTERING: {
      type: "INVESTERING",
      displayName: "Korrigering",
      displayNameLong: "Utbetaling for investering",
      tagName: "INV",
    },
    INNSENDING: {
      type: "INNSENDING",
      displayName: "Innsending",
      displayNameLong: null,
      tagName: null,
    },
  };
