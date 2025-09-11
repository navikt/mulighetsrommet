import z from "zod";

export const FaneinnholdLenkerSchema = z
  .object({
    lenkenavn: z.string().min(3, { error: "Lenkenavnet må være minst 3 tegn" }),
    lenke: z.url({ error: "Du må oppgi en gyldig url" }),
    visKunForVeileder: z.boolean().default(false),
    apneINyFane: z.boolean().default(false),
  })
  .array()
  .nullable()
  .default(null);

export const FaneinnholdSchema = z.object(
  {
    forHvemInfoboks: z.string().nullable().default(null),
    forHvem: z.any().nullable().default(null),
    detaljerOgInnholdInfoboks: z.string().nullable().default(null),
    detaljerOgInnhold: z.any().nullable().default(null),
    pameldingOgVarighetInfoboks: z.string().nullable().default(null),
    pameldingOgVarighet: z.any().nullable().default(null),
    kontaktinfo: z.any().nullable().default(null),
    kontaktinfoInfoboks: z.string().nullable().default(null),
    lenker: FaneinnholdLenkerSchema,
    delMedBruker: z.string().nullable().default(null),
    oppskrift: z.any().nullable().default(null),
  },
  { error: "Det redaksjonelle innholdet må settes på avtalen" },
);
