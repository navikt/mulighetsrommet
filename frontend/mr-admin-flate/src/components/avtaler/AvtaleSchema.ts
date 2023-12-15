import { Avtaletype, Opphav, Tiltakskode } from "mulighetsrommet-api-client";
import z from "zod";

const GyldigUrlHvisVerdi = z.union([
  z.literal(""),
  z.string().trim().url("Du må skrive inn en gyldig nettadresse"),
]);

export const AvtaleSchema = z.object({
  navn: z.string().min(5, "Et avtalenavn må minst være 5 tegn langt"),
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
      sluttDato: z.string({ required_error: "En avtale må ha en sluttdato" }),
    })
    .refine((data) => !data.startDato || !data.sluttDato || data.sluttDato > data.startDato, {
      message: "Startdato må være før sluttdato",
      path: ["startDato"],
    }),
  administratorer: z.string().array().min(1, "Du må velge minst én administrator"),
  url: GyldigUrlHvisVerdi,
  prisbetingelser: z.string().optional(),
  beskrivelse: z
    .string({ required_error: "En avtale trenger en beskrivelse i det redaksjonelle innholdet" })
    .nullable(),
  faneinnhold: z
    .object(
      {
        forHvemInfoboks: z.string().nullable().optional(),
        forHvem: z.any().nullable(),
        detaljerOgInnholdInfoboks: z.string().nullable().optional(),
        detaljerOgInnhold: z.any().nullable(),
        pameldingOgVarighetInfoboks: z.string().nullable().optional(),
        pameldingOgVarighet: z.any().nullable(),
      },
      { required_error: "Det redaksjonelle innholdet må settes på avtalen" },
    )
    .nullable(),
  opphav: z.nativeEnum(Opphav),
});

export type InferredAvtaleSchema = z.infer<typeof AvtaleSchema>;
