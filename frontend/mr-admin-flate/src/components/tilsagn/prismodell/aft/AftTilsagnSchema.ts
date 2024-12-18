import z from "zod";
import { TilsagnType } from "@mr/api-client";
import { tekster } from "@/components/tilsagn/prismodell/Tekster";

export const AftTilsagnSchema = z.object({
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
    .positive({ message: "Antall plasser må være større enn 0" }),
});

export type InferredAftTilsagn = z.infer<typeof AftTilsagnSchema>;
