import z from "zod";
import {
  Tiltakskode,
  Avtaletype,
  OpsjonsmodellType,
  UtdanningslopDbo,
  UtdanningslopDto,
  AmoKategoriseringRequest,
} from "@tiltaksadministrasjon/api-client";

export const avtaleDetaljerSchema = z.object({
  detaljer: z.object({
    navn: z.string().min(5, "Avtalenavn må være minst 5 tegn langt"),
    tiltakskode: z.enum(Tiltakskode, { error: "Du må velge en tiltakstype" }),
    avtaletype: z.enum(Avtaletype, {
      error: "Du må velge en avtaletype",
    }),
    startDato: z.string({
      error: "Du må velge en startdato",
    }),
    sluttDato: z.string().optional().nullable(),
    opsjonsmodell: z.object({
      type: z.enum(OpsjonsmodellType, {
        error: "Du må velge avtalt mulighet for forlengelse",
      }),
      opsjonMaksVarighet: z.string().optional().nullable(),
      customOpsjonsmodellNavn: z.string().optional().nullable(),
    }),
    administratorer: z.array(z.string()).min(1, "Du må velge minst én administrator"),
    sakarkivNummer: z
      .string()
      .nullable()
      .refine(
        (value) => {
          if (!value) return true;
          return /^\d{2}\/\d+$/.test(value);
        },
        {
          message: "Saksnummer må være på formatet 'år/løpenummer'",
        },
      ),
    amoKategorisering: z.custom<AmoKategoriseringRequest>().nullish(),
    utdanningslop: z.custom<UtdanningslopDbo>().nullable(),
    arrangor: z
      .object({
        hovedenhet: z.string(),
        underenheter: z.array(z.string()),
        kontaktpersoner: z.uuid().array(),
      })
      .optional(),
  }),
});

export const validateAvtaledetaljer = (
  ctx: z.RefinementCtx,
  data: z.infer<typeof avtaleDetaljerSchema>,
) => {
  const detaljer = data.detaljer;

  if (detaljer.avtaletype !== Avtaletype.FORHANDSGODKJENT) {
    if (
      detaljer.opsjonsmodell.type === OpsjonsmodellType.ANNET &&
      !detaljer.opsjonsmodell.customOpsjonsmodellNavn
    ) {
      ctx.addIssue({
        code: "custom",
        message: "Du må gi oppsjonsmodellen et navn",
        path: ["opsjonsmodell.customOpsjonsmodellNavn"],
      });
    }
  }
  if (detaljer.sluttDato && detaljer.startDato && detaljer.startDato >= detaljer.sluttDato) {
    ctx.addIssue({
      code: "custom",
      message: "Startdato må være før sluttdato",
      path: ["startDato"],
    });
  }
  if (
    detaljer.tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING &&
    !detaljer.amoKategorisering
  ) {
    ctx.addIssue({
      code: "custom",
      message: "Du må velge en kurstype",
      path: ["amoKategorisering.kurstype"],
    });
  }
  if (detaljer.arrangor?.hovedenhet && detaljer.arrangor.underenheter.length < 0) {
    ctx.addIssue({
      code: "custom",
      message: "Underenheter kan bare være tom dersom hovedenhet er tom",
      path: ["arrangorUnderenheter"],
    });
  }
};

export const avtaleDetaljerFormSchema = avtaleDetaljerSchema.superRefine((data, ctx) => {
  validateAvtaledetaljer(ctx, data);
});

export function toUtdanningslopDbo(data: UtdanningslopDto): UtdanningslopDbo {
  return {
    utdanningsprogram: data.utdanningsprogram.id,
    utdanninger: data.utdanninger.map((utdanning) => utdanning.id),
  };
}

/**
 * Så lenge det mangler validering av utdanningsløp i frontend så trenger vi litt ekstra sanitering av data
 */
export function getUtdanningslop(data: AvtaleDetaljerValues): UtdanningslopDbo | null {
  if (!data.detaljer.utdanningslop?.utdanningsprogram) {
    return null;
  }

  return data.detaljer.utdanningslop;
}

export type AvtaleDetaljerValues = z.infer<typeof avtaleDetaljerFormSchema>;
