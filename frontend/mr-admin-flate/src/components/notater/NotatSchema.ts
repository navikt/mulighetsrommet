import z from "zod";

const antallTegn = 10;
export const NotatSchema = z.object({
  innhold: z
    .string()
    .min(antallTegn, `Et notat må minst være ${antallTegn} tegn langt`),
});

export type inferredAvtalenotatSchema = z.infer<typeof NotatSchema>;
