import z from "zod";
import { FaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";

export const TiltakstypeRedaksjoneltInnholdSchema = z.object({
  beskrivelse: z.string().nullish(),
  faneinnhold: FaneinnholdSchema.nullish(),
  faglenker: z.array(z.object({ id: z.uuid() })),
  kanKombineresMed: z.array(z.uuid()),
});

export type TiltakstypeRedaksjoneltInnholdFormValues = z.infer<
  typeof TiltakstypeRedaksjoneltInnholdSchema
>;
