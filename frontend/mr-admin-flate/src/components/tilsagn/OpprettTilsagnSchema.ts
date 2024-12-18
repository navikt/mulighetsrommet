import z from "zod";
import { TilsagnType } from "@mr/api-client";

const tekster = {
  manglerStartdato: "Du må velge en startdato",
  manglerSluttdato: "Du må velge en sluttdato",
  manglerKostnadssted: "Du må velge et kostnadssted",
  manglerBelop: "Du må skrive inn et beløp for tilsagnet",
} as const;

export const TilsagnSchemaAft = z.object({
  id: z.string().optional().nullable(),
  type: z.nativeEnum(TilsagnType),
  sats: z.number(),
  periodeStart: z
    .string({ required_error: tekster.manglerStartdato })
    .min(10, tekster.manglerStartdato),
  periodeSlutt: z
    .string({ required_error: tekster.manglerSluttdato })
    .min(10, tekster.manglerSluttdato),
  kostnadssted: z
    .string({
      invalid_type_error: tekster.manglerKostnadssted,
      required_error: tekster.manglerKostnadssted,
    })
    .length(4, tekster.manglerKostnadssted),
  antallPlasser: z
    .number({ required_error: "Du må velge antall plasser" })
    .positive({ message: "Antall plasser må være positivt" }),
});

export type InferredTilsagnSchemaAft = z.infer<typeof TilsagnSchemaAft>;

export const TilsagnSchemaFri = z.object({
  id: z.string().optional().nullable(),
  type: z.nativeEnum(TilsagnType),
  periodeStart: z
    .string({ required_error: tekster.manglerStartdato })
    .min(10, tekster.manglerStartdato),
  periodeSlutt: z
    .string({ required_error: tekster.manglerSluttdato })
    .min(10, tekster.manglerSluttdato),
  kostnadssted: z
    .string({
      invalid_type_error: tekster.manglerKostnadssted,
      required_error: tekster.manglerKostnadssted,
    })
    .length(4, tekster.manglerKostnadssted),
  belop: z.number({ required_error: tekster.manglerBelop }),
});

export type InferredTilsagnSchemaFri = z.infer<typeof TilsagnSchemaFri>;
