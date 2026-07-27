import { FaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";
import { z } from "zod";

export const TiltakDokumentSchema = z
  .object({
    navn: z.string().min(1, "Navn er påkrevd"),
    tiltakstypeId: z.string().min(1, "Tiltakstype er påkrevd"),
    stedForGjennomforing: z.string().nullable().optional(),
    arrangorId: z.string().nullable().optional(),
    arrangorKontaktpersoner: z.string().array().default([]),
    administratorer: z.string().array().min(1, "Du må velge minst én administrator"),
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

export type TiltakDokumentFormInput = z.input<typeof TiltakDokumentSchema>;
export type TiltakDokumentFormValues = z.infer<typeof TiltakDokumentSchema>;
