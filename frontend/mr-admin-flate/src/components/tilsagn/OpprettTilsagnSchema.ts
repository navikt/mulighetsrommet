import z, { ZodIssueCode } from "zod";

const tekster = {
  manglerStartdato: "Du må velge en startdato",
  manglerSluttdato: "Du må velg en sluttdato",
  manglerKostnadssted: "Du må velge et kostnadssted",
  manglerBelop: "Du må skrive inn et beløp for tilsagnet",
} as const;

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
    belop: z.number({ required_error: tekster.manglerBelop }).positive(),
  })
  .superRefine((data, ctx) => {
    if (data.periode.slutt < data.periode.start) {
      ctx.addIssue({
        code: ZodIssueCode.custom,
        message: "Sluttdato kan ikke være før startdato",
        path: ["periode.sluttdato"],
      });
    }
  });

export type InferredOpprettTilsagnSchema = z.infer<typeof OpprettTilsagnSchema>;
