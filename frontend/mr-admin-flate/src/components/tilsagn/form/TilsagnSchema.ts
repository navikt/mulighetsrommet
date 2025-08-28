import z from "zod";
import { TilsagnBeregningType, TilsagnType } from "@mr/api-client-v2";

const TilsagnBeregningFriInputLinje = z.object({
  id: z.string(),
  beskrivelse: z.string().nullish(),
  belop: z.number().nullish(),
  antall: z.number().nullish(),
});

export const TilsagnBeregningSchema = z.object({
  type: z.enum(TilsagnBeregningType),
  sats: z.number().nullish(),
  antallTimerOppfolgingPerDeltaker: z.number().nullish(),
  antallPlasser: z.number().nullish(),
  prisbetingelser: z.string().nullish(),
  linjer: z.array(TilsagnBeregningFriInputLinje).nullish(),
});

export const TilsagnSchema = z.object({
  id: z.string().nullish(),
  type: z.enum(TilsagnType),
  gjennomforingId: z.string(),
  kostnadssted: z.string().nullish(),
  beregning: TilsagnBeregningSchema,
  kommentar: z.string().nullish(),
  periodeStart: z.string().nullish(),
  periodeSlutt: z.string().nullish(),
});

export type InferredTilsagn = z.infer<typeof TilsagnSchema>;
