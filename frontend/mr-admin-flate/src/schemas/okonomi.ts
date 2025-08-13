import { Prismodell } from "@mr/api-client-v2";
import z from "zod";

export const okonomiSchema = z.object({
  prisbetingelser: z.string().optional(),
  prismodell: z.enum(Prismodell, { error: "Du må velge en prismodell" }).optional(),
  satser: z.array(
    z.object({
      periodeStart: z
        .string({ error: "Du må legge inn en startdato" })
        .min(10, "Du må legge inn startdato"),
      periodeSlutt: z
        .string({ error: "Du må legge inn en sluttdato" })
        .min(10, "Du må legge inn sluttdato"),
      pris: z.number({ error: "Du må legge inn en pris for perioden" }),
      valuta: z.string(),
    }),
  ),
});
