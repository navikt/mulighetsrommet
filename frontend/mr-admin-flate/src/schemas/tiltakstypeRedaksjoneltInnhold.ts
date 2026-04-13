import z from "zod";

export const TiltakstypeRedaksjoneltInnholdSchema = z.object({
  beskrivelse: z.string().nullish(),
  faneinnhold: z
    .object({
      forHvemInfoboks: z.string().nullish(),
      forHvem: z.any().nullish(),
      detaljerOgInnholdInfoboks: z.string().nullish(),
      detaljerOgInnhold: z.any().nullish(),
      pameldingOgVarighetInfoboks: z.string().nullish(),
      pameldingOgVarighet: z.any().nullish(),
      kontaktinfo: z.any().nullish(),
      kontaktinfoInfoboks: z.string().nullish(),
      lenker: z
        .object({
          lenkenavn: z.string().min(3, { error: "Lenkenavnet må være minst 3 tegn" }),
          lenke: z.url({ error: "Du må oppgi en gyldig url" }),
          visKunForVeileder: z.boolean().default(false),
          apneINyFane: z.boolean().default(false),
        })
        .array()
        .nullable()
        .optional(),
      delMedBruker: z.string().nullish(),
    })
    .nullish(),
  faglenker: z.array(z.object({ id: z.uuid() })),
  kanKombineresMed: z.array(z.uuid()),
});

export type TiltakstypeRedaksjoneltInnholdFormValues = z.infer<
  typeof TiltakstypeRedaksjoneltInnholdSchema
>;
