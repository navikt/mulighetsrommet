import { FaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";
import { z } from "zod";

export const tiltakBeskrivelseDetaljerSchema = z
  .object({
    navn: z.string().min(1, "Navn er påkrevd"),
    tiltaksnummer: z.string().nullable().optional(),
    tiltakstypeId: z.string().min(1, "Tiltakstype er påkrevd"),
    stedForGjennomforing: z.string().nullable().optional(),
    arrangorId: z.string().nullable().optional(),
    arrangorKontaktpersoner: z.string().array().default([]),
    administratorer: z.string().array().min(1, "Du må velge minst én administrator"),
  })
  .loose();

export const tiltakBeskrivelseVeilederinfoSchema = z
  .object({
    veilederinformasjon: z.object({
      beskrivelse: z.string().nullable().optional(),
      faneinnhold: FaneinnholdSchema.nullable().optional(),
      navRegioner: z.string().array().default([]),
      navKontorer: z.string().array().default([]),
      navAndreEnheter: z.string().array().default([]),
      kontaktpersoner: z
        .object({
          navIdent: z.string(),
          beskrivelse: z.string().nullable().optional(),
        })
        .array()
        .default([]),
    }),
  })
  .loose();

export const TiltakBeskrivelseSchema = tiltakBeskrivelseDetaljerSchema.extend(
  tiltakBeskrivelseVeilederinfoSchema.shape,
);

export type TiltakBeskrivelseFormInput = z.input<typeof TiltakBeskrivelseSchema>;
export type TiltakBeskrivelseFormValues = z.infer<typeof TiltakBeskrivelseSchema>;
