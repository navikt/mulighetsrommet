import { Avtaletype, Personopplysning, TiltakskodeArena } from "mulighetsrommet-api-client";
import z from "zod";
import { FaneinnholdSchema } from "./FaneinnholdSchema";

const GyldigUrlHvisVerdi = z.union([
  z.literal(""),
  z.string().trim().url("Du må skrive inn en gyldig nettadresse"),
]);

export const AvtaleSchema = z
  .object({
    navn: z.string(),
    tiltakstype: z.object(
      {
        navn: z.string(),
        arenaKode: z.nativeEnum(TiltakskodeArena),
        id: z.string(),
      },
      { required_error: "Du må velge en tiltakstype" },
    ),
    avtaletype: z.nativeEnum(Avtaletype, {
      required_error: "Du må velge en avtaletype",
    }),
    arrangorOrganisasjonsnummer: z
      .string()
      .min(9, "Du må velge en tiltaksarrangør")
      .max(9, "Du må velge en tiltaksarrangør")
      .regex(/^\d+$/, "Tiltaksarrangør må være et nummer"),
    arrangorUnderenheter: z.string().array().nonempty("Du må velge minst en underenhet"),
    arrangorKontaktpersoner: z.string().uuid().array(),
    navRegioner: z.string().array().nonempty({ message: "Du må velge minst én region" }),
    navEnheter: z.string().array().nonempty({ message: "Du må velge minst én enhet" }),
    startOgSluttDato: z
      .object({
        startDato: z.string({ required_error: "En avtale må ha en startdato" }),
        sluttDato: z.string().optional().nullable(),
      })
      .refine((data) => !data.startDato || !data.sluttDato || data.sluttDato >= data.startDato, {
        message: "Startdato må være før sluttdato",
        path: ["startDato"],
      }),
    administratorer: z.string().array().min(1, "Du må velge minst én administrator"),
    url: GyldigUrlHvisVerdi,
    prisbetingelser: z.string().optional(),
    beskrivelse: z
      .string({ required_error: "En avtale trenger en beskrivelse i det redaksjonelle innholdet" })
      .nullable(),
    faneinnhold: FaneinnholdSchema.nullable(),
    personvernBekreftet: z.boolean({
      required_error: "Du må ta stilling til personvern",
    }).nullable(),
    personopplysninger: z.nativeEnum(Personopplysning).array(),
  })
  .superRefine((data, ctx) => {
    if ([Avtaletype.AVTALE, Avtaletype.RAMMEAVTALE].includes(data.avtaletype) && !data.url) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Avtalen må lenke til Websak og være en gyldig url",
        path: ["url"],
      });
    }
  });

export type InferredAvtaleSchema = z.infer<typeof AvtaleSchema>;
