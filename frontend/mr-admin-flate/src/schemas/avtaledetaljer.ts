import { AmoKategoriseringSchema } from "@/components/redaksjoneltInnhold/AmoKategoriseringSchema";
import z from "zod";
import { AvtaleFormValues } from "./avtale";
import {
  Tiltakskode,
  Avtaletype,
  OpsjonsmodellType,
  UtdanningslopDbo,
  UtdanningslopDto,
} from "@tiltaksadministrasjon/api-client";

export const avtaleDetaljerSchema = z.object({
  detaljer: z.object({
    navn: z.string().min(5, "Avtalenavn må være minst 5 tegn langt"),
    tiltakskode: z.enum(Tiltakskode, { error: "Du må velge en tiltakstype" }),
    avtaletype: z.enum(Avtaletype, {
      error: "Du må velge en avtaletype",
    }),
    startDato: z.string(),
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
    amoKategorisering: AmoKategoriseringSchema.nullish(),
    utdanningslop: z.custom<UtdanningslopDbo>().nullable(),
    arrangorHovedenhet: z.string().optional(),
    arrangorUnderenheter: z.array(z.string()).optional(),
    arrangorKontaktpersoner: z.uuid().array().optional(),
  }),
});

export const validateAvtaledetaljer = (
  ctx: z.RefinementCtx,
  data: z.infer<typeof avtaleDetaljerSchema>,
) => {
  const detaljer = data.detaljer;
  if (
    [Avtaletype.AVTALE, Avtaletype.RAMMEAVTALE].includes(detaljer.avtaletype) &&
    !detaljer.sakarkivNummer
  ) {
    ctx.addIssue({
      code: "custom",
      message: "Du må skrive inn saksnummer til avtalesaken",
      path: ["sakarkivNummer"],
    });
  }
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
  if (!detaljer.arrangorHovedenhet && detaljer.arrangorUnderenheter?.length) {
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
export function getUtdanningslop(data: AvtaleFormValues): UtdanningslopDbo | null {
  if (data.detaljer.tiltakskode !== Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
    return null;
  }

  if (!data.detaljer.utdanningslop?.utdanningsprogram) {
    return null;
  }

  return data.detaljer.utdanningslop;
}
