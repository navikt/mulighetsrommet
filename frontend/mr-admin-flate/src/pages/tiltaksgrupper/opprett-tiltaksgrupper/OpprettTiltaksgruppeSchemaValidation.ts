import { z } from "zod";

export const OpprettTiltaksgruppeSchema = z.object({
  tiltaksgruppenavn: z.string({ required_error: "Tiltaksgruppen må ha et navn" }),
  fraDato: z.string({
    required_error: "Du må sette en fra-dato for tiltaksgruppen",
  }),
  tilDato: z.string({
    required_error: "Du må sette en til-dato for tiltaksgruppen",
  }),
});

