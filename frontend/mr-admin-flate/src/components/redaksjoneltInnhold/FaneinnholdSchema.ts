import z from "zod";

export const FaneinnholdLenkerSchema = z
  .object({
    lenkenavn: z.string().min(3, { message: "Lenkenavnet må være minst 3 tegn" }),
    lenke: z.string().url({ message: "Du må oppgi en gyldig url" }),
    visKunForVeileder: z.boolean().default(false),
    apneINyFane: z.boolean().default(false),
  })
  .array()
  .nullable()
  .optional();

export const FaneinnholdSchema = z.object(
  {
    forHvemInfoboks: z.string().nullable().optional(),
    forHvem: z.any().nullable(),
    detaljerOgInnholdInfoboks: z.string().nullable().optional(),
    detaljerOgInnhold: z.any().nullable(),
    pameldingOgVarighetInfoboks: z.string().nullable().optional(),
    pameldingOgVarighet: z.any().nullable(),
    kontaktinfo: z.any().nullable(),
    kontaktinfoInfoboks: z.string().nullable().optional(),
    lenker: FaneinnholdLenkerSchema,
    delMedBruker: z.string().nullable().optional(),
    kurstittel: z.string().nullable().optional(),
  },
  { required_error: "Det redaksjonelle innholdet må settes på avtalen" },
);

export type InferredFaneinnholdSchema = z.infer<typeof FaneinnholdSchema>;
