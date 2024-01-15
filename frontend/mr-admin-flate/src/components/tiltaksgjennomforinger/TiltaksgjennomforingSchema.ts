import { Opphav, TiltaksgjennomforingOppstartstype } from "mulighetsrommet-api-client";
import z from "zod";

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
      .refine(
        (data) =>
          !data.startDato || !data.sluttDato || new Date(data.sluttDato) > new Date(data.startDato),
        {
          message: "Startdato må være før sluttdato",
          path: ["startDato"],
        },
      ),
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
        navIdent: z.string({
          required_error: "Du må velge en kontaktperson",
        }),
        navEnheter: z
          .string({
            required_error: "Du må velge minst et område",
          })
          .array(),
        beskrivelse: z.string().nullable(),
      })
      .array()
      .optional(),
    tiltaksArrangorUnderenhetOrganisasjonsnummer: z
      .string({
        required_error: "Du må velge en underenhet for tiltaksarrangør",
      })
      .min(1, "Du må velge en underenhet for tiltaksarrangør"),
    stedForGjennomforing: z.string().nullable(),
    arrangorKontaktpersonId: z.string().nullable().optional(),
    administratorer: z
      .string({ required_error: "Du må velge minst én administrator" })
      .array()
      .min(1, "Du må velge minst én administrator"),
    midlertidigStengt: z
      .object({
        erMidlertidigStengt: z.boolean(),
        stengtFra: z.date().optional(),
        stengtTil: z.date().optional(),
      })
      .refine((data) => !data.erMidlertidigStengt || Boolean(data.stengtFra), {
        message: "Midlertidig stengt må ha en start dato",
        path: ["stengtFra"],
      })
      .refine((data) => !data.erMidlertidigStengt || Boolean(data.stengtTil), {
        message: "Midlertidig stengt må ha en til dato",
        path: ["stengtTil"],
      })
      .refine(
        (data) =>
          !data.erMidlertidigStengt ||
          !data.stengtTil ||
          !data.stengtFra ||
          data.stengtTil > data.stengtFra,
        {
          message: "Midlertidig stengt fra dato må være før til dato",
          path: ["stengtFra"],
        },
      ),
    oppstart: z.custom<TiltaksgjennomforingOppstartstype>(
      (val) => !!val,
      "Du må velge oppstartstype",
    ),
    apentForInnsok: z.boolean(),
    beskrivelse: z.string().nullable(),
    faneinnhold: z
      .object({
        forHvemInfoboks: z.string().nullable().optional(),
        forHvem: z.any().nullable(),
        detaljerOgInnholdInfoboks: z.string().nullable().optional(),
        detaljerOgInnhold: z.any().nullable(),
        pameldingOgVarighetInfoboks: z.string().nullable().optional(),
        pameldingOgVarighet: z.any().nullable(),
        kontaktinfo: z.any().nullable(),
      })
      .nullable(),
    opphav: z.nativeEnum(Opphav),
  })
  .superRefine((data, ctx) => {
    if (
      data.startOgSluttDato.sluttDato &&
      data.midlertidigStengt.erMidlertidigStengt &&
      data.midlertidigStengt.stengtTil &&
      data.midlertidigStengt.stengtTil >= new Date(data.startOgSluttDato.sluttDato)
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Stengt til dato må være før sluttdato",
        path: ["midlertidigStengt.stengtTil"],
      });
    }
    if (data.opphav === Opphav.MR_ADMIN_FLATE && !data.startOgSluttDato.sluttDato) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Du må legge til en sluttdato",
        path: ["startOgSluttDato.sluttDato"],
      });

      if (
        data.startOgSluttDato.sluttDato &&
        bareDatoUtenTidspunkt(new Date(data.startOgSluttDato.sluttDato)) <
          bareDatoUtenTidspunkt(new Date())
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Sluttdato kan ikke være før dagens dato",
          path: ["startOgSluttDato.sluttDato"],
        });
      }
      if (
        data.startOgSluttDato.sluttDato &&
        bareDatoUtenTidspunkt(new Date(data.startOgSluttDato.sluttDato)) <
          bareDatoUtenTidspunkt(new Date(data.startOgSluttDato.startDato))
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Sluttdato må være etter startdato",
          path: ["startOgSluttDato.sluttDato"],
        });
      }
    }
  });

function bareDatoUtenTidspunkt(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export type InferredTiltaksgjennomforingSchema = z.infer<typeof TiltaksgjennomforingSchema>;
