import z from "zod";
import { TilsagnBeregningType, TilsagnType } from "@tiltaksadministrasjon/api-client";

const TilsagnBeregningFriInputLinje = z.object({
  id: z.string(),
  beskrivelse: z.string().nullable(),
  belop: z.number().nullable(),
  antall: z.number().nullable(),
});

export const TilsagnBeregningSchema = z.object({
  type: z.enum(TilsagnBeregningType),
  sats: z.number().nullable(),
  antallTimerOppfolgingPerDeltaker: z.number().nullable(),
  antallPlasser: z.number().nullable(),
  prisbetingelser: z.string().nullable(),
  linjer: z.array(TilsagnBeregningFriInputLinje).nullable(),
});

export const TilsagnSchema = z.object({
  id: z.string().nullable(),
  type: z.enum(TilsagnType),
  gjennomforingId: z.string(),
  kostnadssted: z.string().nullable(),
  beregning: TilsagnBeregningSchema,
  kommentar: z.string().nullable(),
  periodeStart: z.string().nullable(),
  periodeSlutt: z.string().nullable(),
});

export type InferredTilsagn = z.infer<typeof TilsagnSchema>;
