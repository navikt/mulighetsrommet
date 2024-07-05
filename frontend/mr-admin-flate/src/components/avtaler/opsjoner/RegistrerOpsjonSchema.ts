import z from "zod";

export const RegistrerOpsjonSchema = z
  .object({
    opsjonsvalg: z.enum(["1", "Annet"], { required_error: "Du må gjøre et opsjonsvalg" }),
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
