import {
  Avtaletype,
  OpsjonsmodellKey,
  Personopplysning,
  Tiltakskode,
  UtdanningslopDbo,
} from "@mr/api-client";
import z from "zod";
import { AmoKategoriseringSchema } from "./AmoKategoriseringSchema";
import { FaneinnholdSchema } from "./FaneinnholdSchema";

export const AvtaleSchema = z
  .object({
    navn: z.string().min(5, "Avtalenavn må være minst 5 tegn langt"),
    tiltakstype: z.object(
      {
        navn: z.string(),
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
        startDato: z
          .string({ required_error: "Du må legge inn startdato for avtalen" })
          .min(10, "Du må legge inn startdato for avtalen"),
        sluttDato: z.string().optional().nullable(),
      })
      .refine((data) => !data.startDato || !data.sluttDato || data.sluttDato >= data.startDato, {
        message: "Startdato må være før sluttdato",
        path: ["startDato"],
      }),
    opsjonsmodellData: z.object({
      opsjonMaksVarighet: z.string().optional().nullable(),
      opsjonsmodell: z.nativeEnum(OpsjonsmodellKey, {
        required_error: "Du må velge avtalt mulighet for forlengelse",
      }),
      customOpsjonsmodellNavn: z.string().optional().nullable(),
    }),
    administratorer: z.string().array().min(1, "Du må velge minst én administrator"),
    websaknummer: z
      .string()
      .nullable()
      .refine(
        (value) => {
          if (!value) return true;
          return /^\d{2}\/\d+$/.test(value);
        },
        {
          message: "Websaknummer må være på formatet 'år/løpenummer'",
        },
      ),
    prisbetingelser: z.string().optional(),
    beskrivelse: z
      .string({ required_error: "En avtale trenger en beskrivelse i det redaksjonelle innholdet" })
      .nullable(),
    faneinnhold: FaneinnholdSchema.nullable(),
    personvernBekreftet: z.boolean({ required_error: "Du må ta stilling til personvern" }),
    personopplysninger: z.nativeEnum(Personopplysning).array(),
    amoKategorisering: AmoKategoriseringSchema.nullish(),
    utdanningslop: z.custom<UtdanningslopDbo>().nullable(),
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

    if (data.avtaletype !== Avtaletype.FORHAANDSGODKJENT) {
      if (!data.opsjonsmodellData.opsjonsmodell) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Du må velge avtalt mulighet for forlengelse",
          path: ["opsjonsmodellData.opsjonsmodell"],
        });
      }

      if (
        data.opsjonsmodellData.opsjonsmodell === OpsjonsmodellKey.ANNET &&
        !data.opsjonsmodellData.customOpsjonsmodellNavn
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Du må gi oppsjonsmodellen et navn",
          path: ["opsjonsmodellData.customOpsjonsmodellNavn"],
        });
      }
    }

    if (!data.startOgSluttDato.startDato) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Du må legge inn startdato for avtalen",
        path: ["startOgSluttDato.startDato"],
      });
    }

    if (
      data.tiltakstype.tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING &&
      !data.amoKategorisering
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Du må velge en kurstype",
        path: ["amoKategorisering.kurstype"],
      });
    }
  });

export type InferredAvtaleSchema = z.infer<typeof AvtaleSchema>;
