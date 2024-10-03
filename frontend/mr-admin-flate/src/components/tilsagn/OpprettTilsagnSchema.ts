import z, { ZodIssueCode } from "zod";

const tekster = {
  manglerStartdato: "Du må velge en startdato",
  manglerSluttdato: "Du må velge en sluttdato",
  manglerKostnadssted: "Du må velge et kostnadssted",
  manglerBelop: "Du må skrive inn et beløp for tilsagnet",
} as const;

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const AFTBeregningSchema = z.object({
  sats: z.number(),
  antallPlasser: z.number(),
  belop: z.number(),
  periodeStart: z
    .string({ required_error: tekster.manglerStartdato })
    .min(10, tekster.manglerStartdato),
  periodeSlutt: z
    .string({ required_error: tekster.manglerSluttdato })
    .min(10, tekster.manglerSluttdato),
});

export type InferredAFTBeregningSchema = z.infer<typeof AFTBeregningSchema>;

export const OpprettTilsagnSchema = z
  .object({
    id: z.string().nullish(),
    periode: z.object({
      start: z
        .string({ required_error: tekster.manglerStartdato })
        .min(10, tekster.manglerStartdato),
      slutt: z
        .string({ required_error: tekster.manglerSluttdato })
        .min(10, tekster.manglerSluttdato),
    }),
    kostnadssted: z
      .string({ required_error: tekster.manglerKostnadssted })
      .length(4, tekster.manglerKostnadssted),
    belop: z.number(),
  })
  .superRefine((data, ctx) => {
    if (data.periode.slutt < data.periode.start) {
      ctx.addIssue({
        code: ZodIssueCode.custom,
        message: "Sluttdato kan ikke være før startdato",
        path: ["periode.slutt"],
      });
    }
  });

export type InferredOpprettTilsagnSchema = z.infer<typeof OpprettTilsagnSchema>;
