import z from "zod";

export const FaneinnholdLenkerSchema = z
  .object({
    lenkenavn: z.string().min(3, { error: "Lenkenavnet må være minst 3 tegn" }),
    lenke: z.string().url({ error: "Du må oppgi en gyldig url" }),
    visKunForVeileder: z.boolean().default(false),
    apneINyFane: z.boolean().default(false),
  })
  .array()
  .nullable()
  .optional();

export const FaneinnholdSchema = z.object(
  {
    forHvemInfoboks: z.string().nullish(),
    forHvem: z.any().nullish(),
    detaljerOgInnholdInfoboks: z.string().nullish(),
    detaljerOgInnhold: z.any().nullish(),
    pameldingOgVarighetInfoboks: z.string().nullish(),
    pameldingOgVarighet: z.any().nullish(),
    kontaktinfo: z.any().nullish(),
    kontaktinfoInfoboks: z.string().nullish(),
    lenker: FaneinnholdLenkerSchema,
    delMedBruker: z.string().nullish(),
  },
  { error: "Det redaksjonelle innholdet må settes på avtalen" },
);

export type InferredFaneinnholdSchema = z.infer<typeof FaneinnholdSchema>;
