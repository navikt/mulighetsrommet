import { Prismodell } from "@mr/api-client-v2";
import z from "zod";

export const okonomiSchema = z.object({
  prisbetingelser: z.string().optional(),
  prismodell: z.nativeEnum(Prismodell).nullable(),
  satser: z.array(
    z.object({
      periodeStart: z.string({ required_error: "Du må legge inn en startdato for perioden" }),
      periodeSlutt: z.string({ required_error: "Du må legge inn en sluttdato for perioden" }),
      pris: z.number({ required_error: "Du må legge inn en pris for perioden" }),
      valuta: z.string(),
    }),
  ),
});

export const validateOkonomi = (ctx: z.RefinementCtx, data: z.infer<typeof okonomiSchema>) => {
  for (let i = 0; i < data.satser.length; i++) {
    const a = data.satser[i];
    for (let j = i + 1; j < data.satser.length; j++) {
      const b = data.satser[j];
      if (a.periodeStart <= b.periodeSlutt && b.periodeStart <= a.periodeSlutt) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Perioder i satser kan ikke overlappe",
          path: [i, "periodeStart"],
        });
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Perioder i satser kan ikke overlappe",
          path: [j, "periodeStart"],
        });
      }
    }
  }
};
