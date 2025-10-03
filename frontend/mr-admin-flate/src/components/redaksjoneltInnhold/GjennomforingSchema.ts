import z from "zod";
import { FaneinnholdSchema } from "./FaneinnholdSchema";
import { STED_FOR_GJENNOMFORING_MAX_LENGTH } from "@/constants";
import { AmoKategoriseringSchema } from "./AmoKategoriseringSchema";
import {
  ArenaMigreringOpphav,
  GjennomforingOppstartstype,
  UtdanningslopDbo,
} from "@tiltaksadministrasjon/api-client";

export const GjennomforingSchema = z
  .object({
    navn: z.string().min(1, "Du må skrive inn tiltaksnavn"),
    avtaleId: z.string(),
    startOgSluttDato: z
      .object({
        startDato: z
          .string({
            error: "Du må legge inn startdato for gjennomføringen",
          })
          .min(8, "Du må legge inn startdato for gjennomføringen"),
        sluttDato: z.string().optional().nullable(),
      })
      .refine((data) => !data.startDato || !data.sluttDato || data.sluttDato >= data.startDato, {
        error: "Startdato må være før sluttdato",
        path: ["startDato"],
      }),
    antallPlasser: z
      .number({
        error: "Du må legge inn antall plasser",
      })
      .int()
      .positive(),
    deltidsprosent: z.number({
      error: "Du må velge deltidsprosent mellom 0 og 100",
    }),
    navRegioner: z.string().array().nonempty({
      error: "Du må velge minst én region",
    }),
    navKontorer: z.string().array(),
    navEnheterAndre: z.string().array(),
    kontaktpersoner: z
      .object({
        navIdent: z.string({ error: "Du må velge en kontaktperson" }),
        beskrivelse: z.string().nullish(),
      })
      .array()
      .optional(),
    arrangorId: z.uuid({
      error: "Du må velge en underenhet for tiltaksarrangør",
    }),
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
          error: `Du kan bare skrive ${STED_FOR_GJENNOMFORING_MAX_LENGTH} tegn i "Sted for gjennomføring"`,
        },
      ),
    arrangorKontaktpersoner: z.uuid().array(),
    administratorer: z
      .string({ error: "Du må velge minst én administrator" })
      .array()
      .min(1, "Du må velge minst én administrator"),
    oppstart: z.custom<GjennomforingOppstartstype>((val) => !!val, "Du må velge oppstartstype"),
    beskrivelse: z.string().nullable(),
    faneinnhold: FaneinnholdSchema.nullable(),
    opphav: z.enum(ArenaMigreringOpphav),
    visEstimertVentetid: z.boolean(),
    estimertVentetid: z
      .object({
        verdi: z.number({
          error: "Du må sette en verdi for estimert ventetid",
        }),
        enhet: z.enum(["uke", "maned"], {
          error: "Du må sette en enhet for estimert ventetid",
        }),
      })
      .nullable(),
    tilgjengeligForArrangorDato: z.string().nullable().optional(),
    amoKategorisering: AmoKategoriseringSchema.nullish(),
    utdanningslop: z.custom<UtdanningslopDbo>().nullable(),
  })
  .check((ctx) => {
    ctx.value.kontaktpersoner?.forEach((kontaktperson, index) => {
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      if (kontaktperson.navIdent === null) {
        ctx.issues.push({
          code: "custom",
          error: "Du må velge en kontaktperson",
          path: [`kontaktpersoner.${index}.navIdent`],
          input: kontaktperson,
        });
      }
    });
  });

export type InferredGjennomforingSchema = z.infer<typeof GjennomforingSchema>;
