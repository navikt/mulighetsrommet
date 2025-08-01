import { AmoKategoriseringSchema } from "@/components/redaksjoneltInnhold/AmoKategoriseringSchema";
import {
  Tiltakskode,
  Avtaletype,
  OpsjonsmodellType,
  UtdanningslopDbo,
  Utdanningslop,
} from "@mr/api-client-v2";
import z from "zod";
import { okonomiSchema, validateOkonomi } from "./okonomi";
import { AvtaleFormValues } from "./avtale";

export const avtaleDetaljerSchema = z.object({
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
  startDato: z
    .string({ error: "Du må legge inn startdato for avtalen" })
    .min(10, "Du må legge inn startdato for avtalen"),
  sluttDato: z.string().nullable(),
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
});

export const arrangorSchema = z.object({
  arrangorHovedenhet: z.string().optional(),
  arrangorUnderenheter: z.array(z.string()).optional(),
  arrangorKontaktpersoner: z.uuid().array().optional(),
});

export const validateArrangor = (ctx: z.RefinementCtx, data: z.infer<typeof arrangorSchema>) => {
  if (!data.arrangorHovedenhet && data.arrangorUnderenheter?.length) {
    ctx.addIssue({
      code: "custom",
      message: "Underenheter kan bare være tom dersom hovedenhet er tom",
      path: ["arrangorUnderenheter"],
    });
  }
};

export const validateAvtaledetaljer = (
  ctx: z.RefinementCtx,
  data: z.infer<typeof avtaleDetaljerSchema>,
) => {
  if (
    [Avtaletype.AVTALE, Avtaletype.RAMMEAVTALE].includes(data.avtaletype) &&
    !data.sakarkivNummer
  ) {
    ctx.addIssue({
      code: "custom",
      message: "Du må skrive inn saksnummer til avtalesaken",
      path: ["sakarkivNummer"],
    });
  }
  if (data.avtaletype !== Avtaletype.FORHANDSGODKJENT) {
    if (!data.opsjonsmodell.type) {
      ctx.addIssue({
        code: "custom",
        message: "Du må velge avtalt mulighet for forlengelse",
        path: ["opsjonsmodell.type"],
      });
    }
    if (
      data.opsjonsmodell.type === OpsjonsmodellType.ANNET &&
      !data.opsjonsmodell.customOpsjonsmodellNavn
    ) {
      ctx.addIssue({
        code: "custom",
        message: "Du må gi oppsjonsmodellen et navn",
        path: ["opsjonsmodell.customOpsjonsmodellNavn"],
      });
    }
  }
  if (data.sluttDato && data.startDato >= data.sluttDato) {
    ctx.addIssue({
      code: "custom",
      message: "Startdato må være før sluttdato",
      path: ["startDato"],
    });
  }
  if (
    data.tiltakstype.tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING &&
    !data.amoKategorisering
  ) {
    ctx.addIssue({
      code: "custom",
      message: "Du må velge en kurstype",
      path: ["amoKategorisering.kurstype"],
    });
  }
};

export const avtaleDetaljerFormSchema = avtaleDetaljerSchema
  .extend(arrangorSchema.shape)
  .extend(okonomiSchema.shape)
  .superRefine((data, ctx) => {
    validateArrangor(ctx, data);
    validateAvtaledetaljer(ctx, data);
    validateOkonomi(ctx, data);
  });

export function toUtdanningslopDbo(data: Utdanningslop): UtdanningslopDbo {
  return {
    utdanningsprogram: data.utdanningsprogram.id,
    utdanninger: data.utdanninger.map((utdanning) => utdanning.id),
  };
}

/**
 * Så lenge det mangler validering av utdanningsløp i frontend så trenger vi litt ekstra sanitering av data
 */
export function getUtdanningslop(data: AvtaleFormValues): UtdanningslopDbo | null {
  if (data.tiltakstype.tiltakskode !== Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
    return null;
  }

  if (!data.utdanningslop?.utdanningsprogram || !data.utdanningslop?.utdanninger) {
    return null;
  }

  return data.utdanningslop;
}
