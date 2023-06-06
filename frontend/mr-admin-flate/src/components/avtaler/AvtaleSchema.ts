import { Avtaletype } from "mulighetsrommet-api-client";
import z from "zod";

const GyldigUrlHvisVerdi = z.union([
  z.literal(""),
  z.string().trim().url("Du må skrive inn en gyldig nettadresse"),
]);

export const AvtaleSchema = z.object({
  avtalenavn: z.string().min(5, "Et avtalenavn må minst være 5 tegn langt"),
  tiltakstype: z.string({ required_error: "Du må velge en tiltakstype" }),
  avtaletype: z.nativeEnum(Avtaletype, {
    required_error: "Du må velge en avtaletype",
  }),
  leverandor: z
    .string()
    .min(9, "Du må velge en leverandør")
    .max(9, "Du må velge en leverandør")
    .regex(/^\d+$/, "Leverandør må være et nummer"),
  leverandorUnderenheter: z.string().array(),
  navRegion: z.string({ required_error: "Du må velge en enhet" }),
  navEnheter: z
    .string()
    .array()
    .nonempty({ message: "Du må velge minst én enhet" }),
  startDato: z
    .date({
      required_error: "En avtale må ha en startdato",
    })
    .nullable()
    .refine((val) => val !== null, {
      message: "En avtale må ha en startdato",
    }),
  sluttDato: z
    .date({
      required_error: "En avtale må ha en sluttdato",
    })
    .nullable()
    .refine((val) => val !== null, {
      message: "En avtale må ha en sluttdato",
    }),
  avtaleansvarlig: z.string({
    required_error: "Du må velge en avtaleansvarlig",
  }),
  url: GyldigUrlHvisVerdi,
  prisOgBetalingsinfo: z.string().optional(),
});

export type inferredSchema = z.infer<typeof AvtaleSchema>;
