import {
  Avtaletype,
  OpsjonsmodellType,
  Personopplysning,
  Prismodell,
  Tiltakskode,
  UtdanningslopDbo,
} from "@mr/api-client-v2";
import z from "zod";
import { AmoKategoriseringSchema } from "./AmoKategoriseringSchema";
import { FaneinnholdSchema } from "./FaneinnholdSchema";

export const AvtaleSchema = z
  .object({
    navn: z.string().min(5, "Avtalenavn må være minst 5 tegn langt"),
    tiltakstype: z.object(
      {
        navn: z.string(),
        tiltakskode: z.enum(Tiltakskode),
        id: z.string(),
      },
      { error: "Du må velge en tiltakstype" },
    ),
    avtaletype: z.enum(Avtaletype, {
      error: "Du må velge en avtaletype",
    }),
    arrangorHovedenhet: z.string().optional(),
    arrangorUnderenheter: z.array(z.string()).optional(),
    arrangorKontaktpersoner: z.uuid().array().optional(),
    navRegioner: z.string().array().nonempty({ error: "Du må velge minst én region" }),
    navKontorer: z.string().array(),
    navAndreEnheter: z.string().array(),
    startOgSluttDato: z
      .object({
        startDato: z
          .string({ error: "Du må legge inn startdato for avtalen" })
          .min(10, "Du må legge inn startdato for avtalen"),
        sluttDato: z.string().optional().nullable(),
      })
      .refine((data) => !data.startDato || !data.sluttDato || data.sluttDato >= data.startDato, {
        error: "Startdato må være før sluttdato",
        path: ["startDato"],
      }),
    opsjonsmodell: z.object({
      type: z.enum(OpsjonsmodellType, {
        error: "Du må velge avtalt mulighet for forlengelse",
      }),
      opsjonMaksVarighet: z.string().nullish(),
      customOpsjonsmodellNavn: z.string().nullish(),
    }),
    administratorer: z.string().array().min(1, "Du må velge minst én administrator"),
    sakarkivNummer: z
      .string()
      .nullable()
      .refine(
        (value) => {
          if (!value) return true;
          return /^\d{2}\/\d+$/.test(value);
        },
        {
          error: "Saksnummer må være på formatet 'år/løpenummer'",
        },
      ),
    prisbetingelser: z.string().optional(),
    beskrivelse: z
      .string({ error: "En avtale trenger en beskrivelse i det redaksjonelle innholdet" })
      .nullable(),
    faneinnhold: FaneinnholdSchema.nullable(),
    personvernBekreftet: z.boolean({ error: "Du må ta stilling til personvern" }),
    personopplysninger: z.enum(Personopplysning).array(),
    amoKategorisering: AmoKategoriseringSchema.nullish(),
    utdanningslop: z.custom<UtdanningslopDbo>().nullable(),
    prismodell: z.enum(Prismodell).nullable(),
    satser: z
      .array(
        z.object({
          periodeStart: z.string({ error: "Du må legge inn en startdato for perioden" }),
          periodeSlutt: z.string({ error: "Du må legge inn en sluttdato for perioden" }),
          pris: z.number({ error: "Du må legge inn en pris for perioden" }),
          valuta: z.string(),
        }),
      )
      .check((ctx) => {
        const satser = ctx.value;
        for (let i = 0; i < satser.length; i++) {
          const a = satser[i];
          for (let j = i + 1; j < satser.length; j++) {
            const b = satser[j];
            if (a.periodeStart <= b.periodeSlutt && b.periodeStart <= a.periodeSlutt) {
              ctx.issues.push({
                code: "custom",
                error: "Perioder i satser kan ikke overlappe",
                path: [i, "periodeStart"],
                input: satser,
              });
              ctx.issues.push({
                code: "custom",
                error: "Perioder i satser kan ikke overlappe",
                path: [j, "periodeStart"],
                input: satser,
              });
            }
          }
        }
      }),
  })
  .check((ctx) => {
    if (ctx.value.avtaletype !== Avtaletype.FORHANDSGODKJENT) {
      if (!ctx.value.opsjonsmodell.type) {
        ctx.issues.push({
          code: "custom",
          error: "Du må velge avtalt mulighet for forlengelse",
          path: ["opsjonsmodell.type"],
          input: ctx.value.opsjonsmodell,
        });
      }

      if (
        ctx.value.opsjonsmodell.type === OpsjonsmodellType.ANNET &&
        !ctx.value.opsjonsmodell.customOpsjonsmodellNavn
      ) {
        ctx.issues.push({
          code: "custom",
          error: "Du må gi oppsjonsmodellen et navn",
          path: ["opsjonsmodell.customOpsjonsmodellNavn"],
          input: ctx.value.opsjonsmodell,
        });
      }
    }

    if (!ctx.value.startOgSluttDato.startDato) {
      ctx.issues.push({
        code: "custom",
        error: "Du må legge inn startdato for avtalen",
        path: ["startOgSluttDato.startDato"],
        input: ctx.value.startOgSluttDato.startDato,
      });
    }

    if (!ctx.value.arrangorHovedenhet && ctx.value.arrangorUnderenheter?.length) {
      ctx.issues.push({
        code: "custom",
        error: "Underenheter can only be empty if hovedenhet is also empty",
        path: ["arrangorUnderenheter"],
        input: ctx.value.arrangorHovedenhet,
      });
    }
  });

export type InferredAvtaleSchema = z.infer<typeof AvtaleSchema>;
