import z from "zod";

const opsjonsvalg = z.enum(["1", "Annet", "Opsjon_skal_ikke_utloses"], {
  error: "Du må gjøre et opsjonsvalg",
});

export type Opsjonsvalg = z.infer<typeof opsjonsvalg>;

export const RegistrerOpsjonSchema = z
  .object({
    opsjonsvalg,
    opsjonsdatoValgt: z.string({ error: "Ny sluttdato for avtalen må settes" }).nullable(),
  })
  .check((ctx) => {
    if (ctx.value.opsjonsvalg === "Annet") {
      if (!ctx.value.opsjonsdatoValgt) {
        ctx.issues.push({
          code: "custom",
          path: ["opsjonsdatoValgt"],
          message: "Ny sluttdato for avtalen må settes",
          input: ctx.value,
        });
      }
    }
  });

export type InferredRegistrerOpsjonSchema = z.infer<typeof RegistrerOpsjonSchema>;
