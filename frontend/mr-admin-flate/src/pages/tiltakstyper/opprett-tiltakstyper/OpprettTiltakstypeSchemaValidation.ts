import { z } from "zod";

const BooleanDefaultFalse = z.boolean().default(false);
const HandlingsplanEnum = z.enum(["AKT", "SOK", "LAG", "TIL"], {
  required_error: "Du må velge en handlingsplan",
});
const RammeAvtaleEnum = z.enum(["SKAL", "KAN", "IKKE"], {
  required_error: "Du må velge en om tiltaket krever en rammeavtale",
});

const AdministrasjonskodeEnum = z.enum(["AMO", "IND", "INST"], {
  required_error: "Du må sette en administrasjonskode",
});

export const OpprettTiltakstypeSchema = z.object({
  tiltakstypenavn: z.string({ required_error: "Tiltakstypen må ha et navn" }),
  fraDato: z.string({
    required_error: "Du må sette en fra-dato for tiltakstypen",
  }),
  tilDato: z.string({
    required_error: "Du må sette en til-dato for tiltakstypen",
  }),
  tiltaksgruppekode: z.string({
    required_error: "Tiltaksgruppekode må være satt",
  }),
  tiltakskode: z.string({ required_error: "Du må skrive inn tiltakskode" }),
  rettTilTiltakspenger: BooleanDefaultFalse,
  administrasjonskode: AdministrasjonskodeEnum,
  kopiAvTilsagnsbrev: BooleanDefaultFalse,
  harAnskaffelse: BooleanDefaultFalse,
  rammeavtale: RammeAvtaleEnum,
  opplaringsgruppe: z
    .string({
      required_error: "Du må velge en opplæringsgruppe",
    })
    .optional(),
  handlingsplan: HandlingsplanEnum,
  harObligatoriskSluttdato: BooleanDefaultFalse,
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

export type HandlingsplanValue = z.infer<typeof HandlingsplanEnum>;
export type RammeavtaleValue = z.infer<typeof RammeAvtaleEnum>;
export type AdministrasjonskodeValue = z.infer<typeof AdministrasjonskodeEnum>;
