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
    antallPlasser: z
      .number({ required_error: "Du må velge antall plasser" })
      .positive({ message: "Antall plasser må være positivt" }),
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
    periodeStart: z
      .string({ required_error: tekster.manglerStartdato })
      .min(10, tekster.manglerStartdato),
    periodeSlutt: z
      .string({ required_error: tekster.manglerSluttdato })
      .min(10, tekster.manglerSluttdato),
    kostnadssted: z
      .string({ required_error: tekster.manglerKostnadssted })
      .length(4, tekster.manglerKostnadssted),
    beregning: TilsagnBeregningSchema,
  })
  .superRefine((data, ctx) => {
    if (data.periodeSlutt < data.periodeStart) {
      ctx.addIssue({
        code: ZodIssueCode.custom,
        message: "Sluttdato kan ikke være før startdato",
        path: ["periodeSlutt"],
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
