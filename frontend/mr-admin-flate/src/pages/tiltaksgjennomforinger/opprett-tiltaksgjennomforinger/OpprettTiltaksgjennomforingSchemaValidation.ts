import { z } from "zod";
import { BooleanDefaultFalse } from "../../OpprettComponents";

export const OpprettTiltaksgjennomforingSchema = z.object({
  tiltaksgjennomforingnavn: z.string({ required_error: "Tiltaksgjennomføringen må ha et navn" }),
  fraDato: z.string({
    required_error: "Du må sette en fra-dato for tiltaksgjennomføringen",
  }),
  tilDato: z.string({
    required_error: "Du må sette en til-dato for tiltaksgjennomføringen",
  }),
  harAutomatiskTilsagnsbrev: BooleanDefaultFalse,
  harStatusBegrunnelseInnsok: BooleanDefaultFalse,
  harStatusHenvisningsbrev: BooleanDefaultFalse,
  harStatusKopibrev: BooleanDefaultFalse,
});
