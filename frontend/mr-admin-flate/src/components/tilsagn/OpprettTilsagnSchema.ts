import z, { ZodIssueCode } from "zod";

const tekster = {
  manglerStartdato: "Du må velge en startdato",
  manglerSluttdato: "Du må velge en sluttdato",
  manglerKostnadssted: "Du må velge et kostnadssted",
  manglerBelop: "Du må skrive inn et beløp for tilsagnet",
} as const;

const TilsagnBeregningSchema = z.discriminatedUnion("type", [
  z.object({
    type: z.literal("AFT"),
    sats: z.number(),
    antallPlasser: z.number(),
    belop: z.number(),
    periodeStart: z
      .string({ required_error: tekster.manglerStartdato })
      .min(10, tekster.manglerStartdato),
    periodeSlutt: z
      .string({ required_error: tekster.manglerSluttdato })
      .min(10, tekster.manglerSluttdato),
  }),
  z.object({
    type: z.literal("FRI"),
    belop: z.number(),
  }),
]);

export const OpprettTilsagnSchema = z
  .object({
    id: z.string().optional().nullable(),
    periode: z.object({
      start: z
        .string({ required_error: tekster.manglerStartdato })
        .min(10, tekster.manglerStartdato),
      slutt: z
        .string({ required_error: tekster.manglerSluttdato })
        .min(10, tekster.manglerSluttdato),
    }),
    kostnadssted: z.string().length(4, tekster.manglerKostnadssted),
    beregning: TilsagnBeregningSchema,
  })
  .superRefine((data, ctx) => {
    if (data.periode.slutt < data.periode.start) {
      ctx.addIssue({
        code: ZodIssueCode.custom,
        message: "Sluttdato kan ikke være før startdato",
        path: ["periode.slutt"],
      });
    }
    if (!data.beregning) {
      ctx.addIssue({
        code: ZodIssueCode.custom,
        message: "Beregning mangler",
        path: ["beregning"],
      });
    }
  });

export type InferredOpprettTilsagnSchema = z.infer<typeof OpprettTilsagnSchema>;
