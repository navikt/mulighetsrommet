import {
  Avtaletype,
  ForerkortKlasse,
  InnholdElement,
  Kurstype,
  Personopplysning,
  Spesifisering,
  Tiltakskode,
  TiltakskodeArena,
} from "mulighetsrommet-api-client";
import z from "zod";
import { FaneinnholdSchema } from "./FaneinnholdSchema";

export const AvtaleSchema = z
  .object({
    navn: z.string(),
    tiltakstype: z.object(
      {
        navn: z.string(),
        arenaKode: z.nativeEnum(TiltakskodeArena),
        tiltakskode: z.nativeEnum(Tiltakskode),
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
        startDato: z.string({ required_error: "Du må legge inn startdato for avtalen" }),
        sluttDato: z.string().optional().nullable(),
      })
      .refine((data) => !data.startDato || !data.sluttDato || data.sluttDato >= data.startDato, {
        message: "Startdato må være før sluttdato",
        path: ["startDato"],
      }),
    administratorer: z.string().array().min(1, "Du må velge minst én administrator"),
    websaknummer: z
      .string()
      .nullable()
      .refine((value) => !value || /^\d{2}\/\d+$/.test(value), {
        message: "Websaknummer må være på formatet 'år/løpenummer'",
      }),
    prisbetingelser: z.string().optional(),
    beskrivelse: z
      .string({ required_error: "En avtale trenger en beskrivelse i det redaksjonelle innholdet" })
      .nullable(),
    faneinnhold: FaneinnholdSchema.nullable(),
    personvernBekreftet: z.boolean({ required_error: "Du må ta stilling til personvern" }),
    personopplysninger: z.nativeEnum(Personopplysning).array(),
    nusData: z
      .object({
        versjon: z.string(),
        utdanningsnivaa: z.string({ required_error: "Du må velge et utdanningsnivå" }),
        utdanningskategorier: z
          .object(
            {
              code: z.string(),
              name: z.string(),
            },
            { required_error: "Du må velge minst én utdanningskategori" },
          )
          .array()
          .optional(),
      })
      .optional(),
    amoKategorisering: z
      .object({
        kurstype: z.nativeEnum(Kurstype, { required_error: "Du må velge en kurstype" }),
        spesifisering: z.nativeEnum(Spesifisering).optional(),
        forerkort: z.nativeEnum(ForerkortKlasse).array().optional(),
        norskprove: z.boolean().nullable().optional(),
        innholdElementer: z.nativeEnum(InnholdElement).array().optional(),
      })
      .optional(),
  })
  .superRefine((data, ctx) => {
    if (
      [Avtaletype.AVTALE, Avtaletype.RAMMEAVTALE].includes(data.avtaletype) &&
      !data.websaknummer
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Du må skrive inn Websaknummer til avtalesaken",
        path: ["websaknummer"],
      });
    }

    if (
      data.tiltakstype.arenaKode === TiltakskodeArena.GRUFAGYRKE &&
      !data.nusData?.utdanningsnivaa
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Du må velge et utdanningsnivå",
        path: ["nusData.utdanningsnivaa"],
      });
    }

    if (
      data.tiltakstype.arenaKode === TiltakskodeArena.GRUFAGYRKE &&
      data.nusData?.utdanningskategorier?.length === 0
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Du må velge minst én utdanningskategori",
        path: ["nusData.utdanningskategorier"],
      });
    }

    if (
      data.tiltakstype.arenaKode === TiltakskodeArena.GRUPPEAMO &&
      data.amoKategorisering?.kurstype &&
      data.amoKategorisering.kurstype !== Kurstype.STUDIESPESIALISERING &&
      !data.amoKategorisering?.spesifisering
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Du må velge en spesifisering",
        path: ["amoKategorisering.spesifisering"],
      });
    }
  });

export type InferredAvtaleSchema = z.infer<typeof AvtaleSchema>;
