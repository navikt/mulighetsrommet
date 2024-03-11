import { Avtaletype, Tiltakskode } from "mulighetsrommet-api-client";
import z from "zod";
import { FaneinnholdSchema } from "./FaneinnholdSchema";

const GyldigUrlHvisVerdi = z.union([
  z.literal(""),
  z.string().trim().url("Du må skrive inn en gyldig nettadresse"),
]);

export const AvtaleSchema = z.object({
  navn: z.string(),
  tiltakstype: z.object(
    {
      navn: z.string(),
      arenaKode: z.nativeEnum(Tiltakskode),
      id: z.string(),
    },
    { required_error: "Du må velge en tiltakstype" },
  ),
  avtaletype: z.nativeEnum(Avtaletype, {
    required_error: "Du må velge en avtaletype",
  }),
  leverandor: z
    .string()
    .min(9, "Du må velge en leverandør")
    .max(9, "Du må velge en leverandør")
    .regex(/^\d+$/, "Leverandør må være et nummer"),
  leverandorUnderenheter: z.string().array().nonempty("Du må velge minst en underenhet"),
  leverandorKontaktpersonId: z.string().optional(),
  navRegioner: z.string().array().nonempty({ message: "Du må velge minst én region" }),
  navEnheter: z.string().array().nonempty({ message: "Du må velge minst én enhet" }),
  startOgSluttDato: z
    .object({
      startDato: z.string({ required_error: "En avtale må ha en startdato" }),
      sluttDato: z.string().optional().nullable(),
    })
    .refine((data) => data.startDato && data.sluttDato && data.sluttDato >= data.startDato, {
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
});

export type InferredAvtaleSchema = z.infer<typeof AvtaleSchema>;
