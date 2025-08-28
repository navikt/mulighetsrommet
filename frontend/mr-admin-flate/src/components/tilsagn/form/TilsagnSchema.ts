import z from "zod";
import { TilsagnType } from "@mr/api-client-v2";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";

const TilsagnBeregningFriInputLinje = z.object({
  id: z.string(),
  beskrivelse: z.string(),
  belop: z
    .number({
      error: "Beløp mangler",
    })
    .positive({ error: "Beløp må være positivt" }),
  antall: z
    .number({
      error: "Antall mangler",
    })
    .positive({ error: "Antall må være positivt" }),
});

export const TilsagnBeregningSchema = z.discriminatedUnion("type", [
  z.object({
    type: z.literal("FAST_SATS_PER_TILTAKSPLASS_PER_MANED"),
    sats: z.number({
      error: "Sats mangler",
    }),
    periode: z.object({
      start: z
        .string({ error: tilsagnTekster.manglerStartdato })
        .min(10, tilsagnTekster.manglerStartdato),
      slutt: z
        .string({ error: tilsagnTekster.manglerSluttdato })
        .min(10, tilsagnTekster.manglerSluttdato),
    }),
    antallPlasser: z
      .number({
        error: "Antall plasser mangler",
      })
      .positive({ error: "Antall plasser må være positivt" }),
  }),
  z.object({
    type: z.literal("PRIS_PER_MANEDSVERK"),
    sats: z.number({
      error: "Sats mangler",
    }),
    periode: z.object({
      start: z
        .string({ error: tilsagnTekster.manglerStartdato })
        .min(10, tilsagnTekster.manglerStartdato),
      slutt: z
        .string({ error: tilsagnTekster.manglerSluttdato })
        .min(10, tilsagnTekster.manglerSluttdato),
    }),
    antallPlasser: z
      .number({
        error: "Antall plasser mangler",
      })
      .positive({ error: "Antall plasser må være positivt" }),
    prisbetingelser: z.string().nullable(),
  }),
  z.object({
    type: z.literal("PRIS_PER_UKESVERK"),
    sats: z.number({
      error: "Sats er påkrevd",
    }),
    periode: z.object({
      start: z
        .string({ error: tilsagnTekster.manglerStartdato })
        .min(10, tilsagnTekster.manglerStartdato),
      slutt: z
        .string({ error: tilsagnTekster.manglerSluttdato })
        .min(10, tilsagnTekster.manglerSluttdato),
    }),
    antallPlasser: z
      .number({
        error: "Antall plasser mangler",
      })
      .positive({ error: "Antall plasser må være positivt" }),
    prisbetingelser: z.string().nullable(),
  }),
  z.object({
    type: z.literal("PRIS_PER_TIME_OPPFOLGING"),
    sats: z.number({
      error: "Sats er påkrevd",
    }),
    periode: z.object({
      start: z
        .string({ error: tilsagnTekster.manglerStartdato })
        .min(10, tilsagnTekster.manglerStartdato),
      slutt: z
        .string({ error: tilsagnTekster.manglerSluttdato })
        .min(10, tilsagnTekster.manglerSluttdato),
    }),
    antallPlasser: z
      .number({
        error: "Antall plasser mangler",
      })
      .positive({ error: "Antall plasser må være positivt" }),
    antallTimerOppfolgingPerDeltaker: z
      .number({
        error: "Antall timer oppfølging per deltaker mangler",
      })
      .positive({ error: "Antall timer oppfølging per deltaker må være positivt" }),
    prisbetingelser: z.string().nullable(),
  }),
  z.object({
    type: z.literal("FRI"),
    linjer: z.array(TilsagnBeregningFriInputLinje),
    prisbetingelser: z.string().nullable(),
  }),
]);

export const TilsagnSchema = z
  .object({
    id: z.string().optional().nullable(),
    gjennomforingId: z.string(),
    type: z.enum(TilsagnType),
    periodeStart: z
      .string({ error: tilsagnTekster.manglerStartdato })
      .min(10, tilsagnTekster.manglerStartdato),
    periodeSlutt: z
      .string({ error: tilsagnTekster.manglerSluttdato })
      .min(10, tilsagnTekster.manglerSluttdato),
    kostnadssted: z
      .string({
        error: tilsagnTekster.manglerKostnadssted,
      })
      .length(4, tilsagnTekster.manglerKostnadssted),
    beregning: TilsagnBeregningSchema,
    kommentar: z.string().nullable(),
  })
  .refine(
    (data) => {
      const start = new Date(data.periodeStart);
      const slutt = new Date(data.periodeSlutt);
      return start <= slutt;
    },
    {
      error: "Periodestart må være før periodeslutt",
      path: ["periodeStart"],
    },
  )
  .refine(
    (data) => {
      const start = new Date(data.periodeStart);
      const slutt = new Date(data.periodeSlutt);
      return slutt >= start;
    },
    {
      error: "Periodeslutt må være etter periodestart",
      path: ["periodeSlutt"],
    },
  );

export type InferredTilsagn = z.infer<typeof TilsagnSchema>;
