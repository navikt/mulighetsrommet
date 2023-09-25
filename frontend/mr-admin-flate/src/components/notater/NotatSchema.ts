import z from "zod";

const minAntallTegn = 10;
const maxAntallTegn = 500;

export const NotatSchema = z.object({
  innhold: z
    .string()
    .min(minAntallTegn, `Et notat må minst være ${minAntallTegn} tegn langt`)
    .max(maxAntallTegn, `Et notat kan ikke være mer enn ${maxAntallTegn} tegn langt`),
});

export type inferredNotatSchema = z.infer<typeof NotatSchema>;
