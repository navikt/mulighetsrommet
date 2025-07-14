import { z } from "zod";

export const UtbetalingSchema = z.object({
  kostnadsfordeling: z
    .object({
      tilsagnId: z.string(),
      belop: z.number({ error: "Du må velge beløp" }),
    })
    .array(),
});

export type InferredUtbetalingSchema = z.infer<typeof UtbetalingSchema>;
