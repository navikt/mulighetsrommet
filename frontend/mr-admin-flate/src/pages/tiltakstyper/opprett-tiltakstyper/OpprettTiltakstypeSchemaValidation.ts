import { z } from "zod";

const BooleanDefaultFalse = z.boolean().default(false);
export const OpprettTiltakstypeSchema = z.object({
  tiltakstypenavn: z.string({ required_error: "Tiltakstypen må ha et navn" }),
  fraDato: z.date({
    required_error: "Du må sette en fra-dato for tiltakstypen",
  }),
  tilDato: z.date({
    required_error: "Du må sette en til-dato for tiltakstypen",
  }),
  tiltaksgruppekode: z.string({
    required_error: "Tiltaksgruppekode må være satt",
  }),
  tiltakskode: z.string({ required_error: "Tiltakskode må være satt" }),
  rettTilTiltakspenger: BooleanDefaultFalse,
  administrasjonskode: z.string({
    required_error: "Du må sette en administrasjonskode",
  }),
  kopiAvTilsagnsbrev: BooleanDefaultFalse,
  arkivkode: z.string({ required_error: "Du må sette en arkivkode" }),
  harAnskaffelse: BooleanDefaultFalse,
  rammeavtale: z.string({ required_error: "Du må velge en rammeavtale" }),
  opplaringsgruppe: z.string({
    required_error: "Du må velge en opplæringsgruppe",
  }),
  handlingsplan: z.string({ required_error: "Du må velge en handlingsplan" }),
  harObligatoriskSluttdato: BooleanDefaultFalse,
  varighet: z.string({ required_error: "Du må sette en varighet" }),
  harStatusSluttdato: BooleanDefaultFalse,
  harStatusMeldeplikt: BooleanDefaultFalse,
  harStatusVedtak: BooleanDefaultFalse,
  harStatusIAAvtale: BooleanDefaultFalse,
  harStatusTilleggstonad: BooleanDefaultFalse,
  harStatusUtdanning: BooleanDefaultFalse,
  harAutomatiskTilsagnsbrev: BooleanDefaultFalse,
  harStatusBegrunnelseInnsok: BooleanDefaultFalse,
  harStatusHenvisningsbrev: BooleanDefaultFalse,
  harStatusKopibrev: BooleanDefaultFalse,
});

export type OptionalSchemaValues = Partial<
  z.infer<typeof OpprettTiltakstypeSchema>
>;
export type SchemaValues = z.infer<typeof OpprettTiltakstypeSchema>;
