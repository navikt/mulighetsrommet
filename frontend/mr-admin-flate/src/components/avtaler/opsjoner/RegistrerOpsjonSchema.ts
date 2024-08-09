import z from "zod";

const opsjonsvalg = z.enum(["1", "Annet", "Opsjon_skal_ikke_utloses"], {
  required_error: "Du må gjøre et opsjonsvalg",
});

export type Opsjonsvalg = z.infer<typeof opsjonsvalg>;

export const RegistrerOpsjonSchema = z
  .object({
    opsjonsvalg,
    opsjonsdatoValgt: z.string({ required_error: "Ny sluttdato for avtalen må settes" }).optional(),
  })
  .superRefine((data, ctx) => {
    if (data.opsjonsvalg === "Annet") {
      if (!data.opsjonsdatoValgt) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["opsjonsdatoValgt"],
          message: "Ny sluttdato for avtalen må settes",
        });
      }
    }
  });

export type InferredRegistrerOpsjonSchema = z.infer<typeof RegistrerOpsjonSchema>;
