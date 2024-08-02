import z from "zod";

export const OpprettTilsagnSchema = z.object({
  periode: z.object({
    start: z
      .string({ required_error: "Tilsagnet må ha en startdato" })
      .min(10, "Tilsagnet må ha en startdato"),
    slutt: z
      .string({ required_error: "Tilsagn må ha en sluttdato" })
      .min(10, "Tilsagnet må ha en sluttdato"),
  }),
  kostnadssted: z.string().length(4, "Du må sette et kostnadssted"),
  arrangorOrganisasjonsnummer: z
    .string({
      required_error: "Organisasjonsnummer til arrangør er ikke satt",
    })
    .length(9, "Organisasjonsnummer må være 9 tegn"),
  belop: z.number({ required_error: "Du må sette et beløp for tilsagnet" }).positive(),
  beslutter: z.string().length(7, "Du må velge en beslutter"),
});

export type InferredOpprettTilsagnSchema = z.infer<typeof OpprettTilsagnSchema>;
