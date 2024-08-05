import z from "zod";

const tekster = {
  manglerStartdato: "Du må velge en startdato",
  manglerSluttdato: "Du må velg en sluttdato",
  manglerKostnadssted: "Du må velge et kostnadssted",
  manglerBelop: "Du må skrive inn et beløp for tilsagnet",
} as const;

export const OpprettTilsagnSchema = z.object({
  periode: z.object({
    start: z.string({ required_error: tekster.manglerStartdato }).min(10, tekster.manglerStartdato),
    slutt: z.string({ required_error: tekster.manglerSluttdato }).min(10, tekster.manglerSluttdato),
  }),
  kostnadssted: z.string().length(4, tekster.manglerKostnadssted),
  belop: z.number({ required_error: tekster.manglerBelop }).positive(),
});

export type InferredOpprettTilsagnSchema = z.infer<typeof OpprettTilsagnSchema>;
