import { Opphav, TiltaksgjennomforingOppstartstype } from "mulighetsrommet-api-client";
import z from "zod";
import { FaneinnholdSchema } from "./FaneinnholdSchema";
import { STED_FOR_GJENNOMFORING_MAX_LENGTH } from "../../constants";

export const TiltaksgjennomforingSchema = z
  .object({
    navn: z.string().min(1, "Du må skrive inn tittel"),
    avtaleId: z.string(),
    startOgSluttDato: z
      .object({
        startDato: z.string({
          required_error: "En gjennomføring må ha en startdato",
        }),
        sluttDato: z.string().optional().nullable(),
      })
      .refine((data) => !data.startDato || !data.sluttDato || data.sluttDato >= data.startDato, {
        message: "Startdato må være før sluttdato",
        path: ["startDato"],
      }),
    antallPlasser: z
      .number({
        invalid_type_error:
          "Du må skrive inn antall plasser for gjennomføringen som et positivt heltall",
      })
      .int()
      .positive(),
    deltidsprosent: z.number({
      invalid_type_error: "Deltidsprosent må være et tall mellom 0 og 100",
      required_error: "Deltidsprosent er påkrevd",
    }),
    navRegion: z.string({ required_error: "Du må velge én region" }),
    navEnheter: z.string().array().nonempty({
      message: "Du må velge minst én enhet",
    }),
    kontaktpersoner: z
      .object({
        navIdent: z.string({ required_error: "Velg kontaktperson" }),
        navEnheter: z
          .string({ required_error: "Velg NAV-enheter som kontaktpersonen er tilgjengelig for" })
          .array(),
        beskrivelse: z.string().nullable().optional(),
      })
      .array()
      .optional(),
    arrangorId: z
      .string({
        required_error: "Du må velge en underenhet for tiltaksarrangør",
      })
      .uuid("Du må velge en underenhet for tiltaksarrangør"),
    stedForGjennomforing: z
      .string()
      .nullable()
      .refine(
        (val) => {
          if (!val) {
            return true;
          }
          return val.length <= STED_FOR_GJENNOMFORING_MAX_LENGTH;
        },
        {
          message: `Du kan bare skrive ${STED_FOR_GJENNOMFORING_MAX_LENGTH} tegn i "Sted for gjennomføring"`,
        },
      ),
    arrangorKontaktpersoner: z.string().uuid().array(),
    administratorer: z
      .string({ required_error: "Du må velge minst én administrator" })
      .array()
      .min(1, "Du må velge minst én administrator"),
    oppstart: z.custom<TiltaksgjennomforingOppstartstype>(
      (val) => !!val,
      "Du må velge oppstartstype",
    ),
    apentForInnsok: z.boolean().default(true),
    beskrivelse: z.string().nullable(),
    faneinnhold: FaneinnholdSchema.nullable(),
    opphav: z.nativeEnum(Opphav),
    visEstimertVentetid: z.boolean(),
    estimertVentetid: z
      .object({
        verdi: z.number({
          required_error: "Du må sette en verdi for estimert ventetid",
          invalid_type_error: "Du må sette en verdi for estimert ventetid",
        }),
        enhet: z.enum(["uke", "maned"], {
          required_error: "Du må sette en enhet for estimert ventetid",
          invalid_type_error: "Du må sette en enhet for estimert ventetid",
        }),
      })
      .nullable(),
    tilgjengeligForArrangorFraOgMedDato: z.string().nullable().optional(),
    nusData: z
      .object({
        versjon: z.string(),
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
  })
  .superRefine((data, ctx) => {
    data.kontaktpersoner?.forEach((kontaktperson, index) => {
      if (kontaktperson.navIdent == null) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Du må velge en kontaktperson",
          path: [`kontaktpersoner.${index}.navIdent`],
        });
      }
      if (kontaktperson.navEnheter.length === 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Du må velge minst én enhet",
          path: [`kontaktpersoner.${index}.navEnheter`],
        });
      }
    });
  });

export type InferredTiltaksgjennomforingSchema = z.infer<typeof TiltaksgjennomforingSchema>;
