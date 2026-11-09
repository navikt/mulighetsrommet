import { useCallback } from "react";
import { z } from "zod";

const APP_NAME = "tiltaksadministrasjon-flate";
const nonEmptyString = z.string().trim().min(1);

export const umamiEventSchemas = {
  FILTER_ENDRET: z
    .object({
      appnavn: z.literal(APP_NAME),
      handling: z.enum(["valgt", "fjernet"]),
      kilde: z.enum(["enkeltvalg", "velg_alle", "fjern_alle", "nullstill"]),
      kategori: nonEmptyString,
      filternavn: nonEmptyString,
      subFilter: nonEmptyString.optional(),
      kontekst: nonEmptyString,
    })
    .strict(),
  FILTERPANEL_ÅPNET: z
    .object({
      appnavn: z.literal(APP_NAME),
      kontekst: nonEmptyString,
    })
    .strict(),
  ACCORDION_ÅPNET: z
    .object({
      appnavn: z.literal(APP_NAME),
      tittel: nonEmptyString,
      kontekst: nonEmptyString,
    })
    .strict(),
  ACCORDION_LUKKET: z
    .object({
      appnavn: z.literal(APP_NAME),
      tittel: nonEmptyString,
      kontekst: nonEmptyString,
    })
    .strict(),
};

type HendelseDataMap = {
  [K in keyof typeof umamiEventSchemas]: Omit<z.output<(typeof umamiEventSchemas)[K]>, "appnavn">;
};

export type FilterHandling = HendelseDataMap["FILTER_ENDRET"]["handling"];
export type FilterKilde = HendelseDataMap["FILTER_ENDRET"]["kilde"];
export type FilterEndring = Omit<HendelseDataMap["FILTER_ENDRET"], "kontekst">;

type UmamiEvent = {
  [K in keyof HendelseDataMap]: { type: K } & HendelseDataMap[K];
}[keyof HendelseDataMap];

export function useUmami() {
  const logUmamiHendelse = useCallback((event: UmamiEvent) => {
    const { type, ...data } = event;
    if (typeof window !== "undefined" && window.umami) {
      window.umami.track(type, {
        appnavn: APP_NAME,
        ...data,
      });
    }
  }, []);

  return { logUmamiHendelse };
}
