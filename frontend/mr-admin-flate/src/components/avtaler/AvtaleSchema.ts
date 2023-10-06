import { Avtaletype, Opphav, Tiltakskode } from "mulighetsrommet-api-client";
import z from "zod";
import { TILTAK_MED_AVTALE_FRA_MULIGHETSROMMET } from "../../utils/tiltakskoder";

const GyldigUrlHvisVerdi = z.union([
  z.literal(""),
  z.string().trim().url("Du må skrive inn en gyldig nettadresse"),
]);

export const AvtaleSchema = z
  .object({
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
    navRegion: z.string({ required_error: "Du må velge en enhet" }),
    navEnheter: z.string().array().nonempty({ message: "Du må velge minst én enhet" }),
    startOgSluttDato: z
      .object({
        startDato: z.date({ required_error: "En avtale må ha en startdato" }),
        sluttDato: z.date({ required_error: "En avtale må ha en sluttdato" }),
      })
      .refine((data) => !data.startDato || !data.sluttDato || data.sluttDato > data.startDato, {
        message: "Startdato må være før sluttdato",
        path: ["startDato"],
      }),
    administrator: z.string().refine((data) => data.length > 0, {
      message: "Du må velge en administrator",
    }),
    url: GyldigUrlHvisVerdi,
    prisbetingelser: z.string().optional(),
    opphav: z.nativeEnum(Opphav),
  })
  .refine(
    (avtale) => {
      if (avtale.opphav !== Opphav.MR_ADMIN_FLATE) {
        return true;
      }

      return TILTAK_MED_AVTALE_FRA_MULIGHETSROMMET.includes(avtale.tiltakstype.arenaKode);
    },
    {
      path: ["tiltakstype"],
      message: "Tiltakstype må være AFT eller VTA når avtalen opprettes utenfor Arena",
    },
  );

export type InferredAvtaleSchema = z.infer<typeof AvtaleSchema>;
