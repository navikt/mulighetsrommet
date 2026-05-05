import { FaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";
import z from "zod";
import {
  AmoKategoriseringRequest,
  Avtaletype,
  OpsjonsmodellType,
  PersonopplysningType,
  PrismodellType,
  Tiltakskode,
  UtdanningslopDbo,
  UtdanningslopDto,
  Valuta,
} from "@tiltaksadministrasjon/api-client";

export const avtaleDetaljerSchema = z.object({
  detaljer: z.object({
    navn: z
      .string({ error: "Du må skrive inn navn" })
      .min(5, "Avtalenavn må være minst 5 tegn langt"),
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

export type AvtaleDetaljerInputValues = z.infer<typeof avtaleDetaljerFormSchema>;
export type AvtaleDetaljerOutputValues = z.infer<typeof avtaleDetaljerFormSchema>;

export const PrismodellSchema = z.object({
  prismodeller: z.array(
    z
      .object({
        id: z.uuid().optional(),
        prisbetingelser: z.string().nullable(),
        type: z.enum(PrismodellType, { error: "Du må velge en prismodell" }),
        valuta: z.enum(Valuta, { error: "Du må velge en valuta" }),
        tilsagnPerDeltaker: z.boolean(),
        satser: z
          .array(
            z.object({
              gjelderFra: z.string().min(1, { message: "Gjelder fra må være satt" }),
              gjelderTil: z.string().nullable(),
              pris: z
                .number({ error: "Pris må være satt" })
                .min(1, { message: "Pris må være positiv" }),
            }),
          )
          .nullable(),
      })
      .superRefine((data, ctx) => {
        if (
          ![PrismodellType.ANNEN_AVTALT_PRIS].includes(data.type) &&
          (!data.satser || data.satser.length === 0)
        ) {
          ctx.addIssue({
            code: "custom",
            message: "Du må legge til minst én sats",
            path: ["satser"],
          });
        }
      }),
  ),
});

export type PrismodellValues = z.infer<typeof PrismodellSchema>;

export const VeilederinformasjonSchema = z.object({
  beskrivelse: z.string().nullable(),
  faneinnhold: FaneinnholdSchema.nullable(),
  navRegioner: z.string().array().nonempty({ message: "Du må velge minst én region" }),
  navKontorer: z.string().array(),
  navAndreEnheter: z.string().array(),
});

export const VeilederinformasjonStepSchema = z.object({
  veilederinformasjon: VeilederinformasjonSchema,
});

export type VeilederinfoInputValues = z.input<typeof VeilederinformasjonStepSchema>;
export type VeilederinfoOutputValues = z.infer<typeof VeilederinformasjonStepSchema>;

export const PersonopplysningerSchema = z.object({
  personvern: z.object({
    personvernBekreftet: z.boolean({ error: "Du må ta stilling til personvern" }),
    personopplysninger: z.enum(PersonopplysningType).array(),
  }),
});

export type PersonopplysningerInputValues = z.input<typeof PersonopplysningerSchema>;
export type PersonopplysningerOutputValues = z.infer<typeof PersonopplysningerSchema>;

export const avtaleFormSchema = avtaleDetaljerSchema
  .extend(PrismodellSchema.shape)
  .extend(PersonopplysningerSchema.shape)
  .extend(VeilederinformasjonStepSchema.shape)
  .superRefine((data, ctx) => {
    validateAvtaledetaljer(ctx, data);

    if (
      data.detaljer.avtaletype !== Avtaletype.FORHANDSGODKJENT &&
      data.prismodeller.length === 0
    ) {
      ctx.addIssue({
        code: "custom",
        message: "Du må legge til minst én prismodell",
        path: ["prismodeller"],
      });
    }
  });

export type AvtaleFormInput = z.input<typeof avtaleFormSchema>;
export type AvtaleFormValues = z.infer<typeof avtaleFormSchema>;
