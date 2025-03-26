import z from "zod";
import { TilsagnType } from "@mr/api-client-v2";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";

const TilsagnBeregningSchema = z.discriminatedUnion("type", [
  z.object({
    type: z.literal("FORHANDSGODKJENT"),
    sats: z.number({
      invalid_type_error: "Sats mangler",
      required_error: "Sats er påkrevd",
    }),
    periode: z.object({
      start: z
        .string({ required_error: tilsagnTekster.manglerStartdato })
        .min(10, tilsagnTekster.manglerStartdato),
      slutt: z
        .string({ required_error: tilsagnTekster.manglerSluttdato })
        .min(10, tilsagnTekster.manglerSluttdato),
    }),
    antallPlasser: z
      .number({
        invalid_type_error: "Antall plasser mangler",
        required_error: "Antall plasser mangler",
      })
      .positive({ message: "Antall plasser må være positivt" }),
  }),
  z.object({
    type: z.literal("FRI"),
    belop: z
      .number({
        invalid_type_error: "Beløp mangler",
        required_error: "Beløp mangler",
      })
      .positive({ message: "Beløp må være positivt" }),
  }),
]);

export const TilsagnSchema = z
  .object({
    id: z.string().optional().nullable(),
    gjennomforingId: z.string(),
    type: z.nativeEnum(TilsagnType),
    periodeStart: z
      .string({ required_error: tilsagnTekster.manglerStartdato })
      .min(10, tilsagnTekster.manglerStartdato),
    periodeSlutt: z
      .string({ required_error: tilsagnTekster.manglerSluttdato })
      .min(10, tilsagnTekster.manglerSluttdato),
    kostnadssted: z
      .string({
        invalid_type_error: tilsagnTekster.manglerKostnadssted,
        required_error: tilsagnTekster.manglerKostnadssted,
      })
      .length(4, tilsagnTekster.manglerKostnadssted),
    beregning: TilsagnBeregningSchema,
  })
  .refine(
    (data) => {
      const start = new Date(data.periodeStart);
      const slutt = new Date(data.periodeSlutt);
      if (start > slutt) {
        return false;
      }
      return true;
    },
    {
      message: "Periodestart må være før periodeslutt",
      path: ["periodeStart"],
    },
  )
  .refine(
    (data) => {
      const start = new Date(data.periodeStart);
      const slutt = new Date(data.periodeSlutt);
      if (slutt < start) {
        return false;
      }
      return true;
    },
    {
      message: "Periodeslutt må være etter periodestart",
      path: ["periodeSlutt"],
    },
  );

export type InferredTilsagn = z.infer<typeof TilsagnSchema>;
