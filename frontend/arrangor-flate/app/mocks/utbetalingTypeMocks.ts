import { UtbetalingTypeDto } from "@arrangor-utbetalinger/api-client";

export const utbetalingType: Record<
  "KORRIGERING" | "INVESTERING" | "INNSENDING",
  UtbetalingTypeDto
> = {
  KORRIGERING: {
    displayName: "Korrigering",
    displayNameLong: null,
    tagName: "KOR",
  },
  INVESTERING: {
    displayName: "Investering",
    displayNameLong: "Utbetaling for investering",
    tagName: "INV",
  },
  INNSENDING: {
    displayName: "Innsending",
    displayNameLong: null,
    tagName: null,
  },
};
