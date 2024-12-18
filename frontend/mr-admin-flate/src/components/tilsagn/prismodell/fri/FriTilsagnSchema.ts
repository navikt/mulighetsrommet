import z from "zod";
import { TilsagnType } from "@mr/api-client";
import { tekster } from "@/components/tilsagn/prismodell/Tekster";

export const FriTilsagnSchema = z.object({
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

export type InferredFriTilsagn = z.infer<typeof FriTilsagnSchema>;
