import { z } from "zod";

const BooleanDefaultFalse = z.boolean().default(false);
const HandlingsplanEnum = z.enum(["SOK", "LAG", "TIL"], {
  required_error: "Du må velge en handlingsplan",
});
const RammeAvtaleEnum = z.enum(["SKAL", "KAN", "IKKE"], {
  required_error: "Du må velge en om tiltaket krever en rammeavtale",
});

const TiltakskodeEnum = z.enum([
  "ARBFORB",
  "ARBRRHDAG",
  "AVKLARAG",
  "DIGIOPPARB",
  "FORSAMOGRU",
  "FORSFAGGRU",
  "GRUFAGYRKE",
  "GRUPPEAMO",
  "INDJOBSTOT",
  "INDOPPFAG",
  "INDOPPRF",
  "IPSUNG",
  "JOBBK",
  "UTVAOONAV",
  "UTVOPPFOPL",
  "VASV",
]);

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
  tiltakskode: TiltakskodeEnum,
  rettTilTiltakspenger: BooleanDefaultFalse,
  administrasjonskode: z.string({
    required_error: "Du må sette en administrasjonskode",
  }),
  kopiAvTilsagnsbrev: BooleanDefaultFalse,
  arkivkode: z.string({ required_error: "Du må sette en arkivkode" }),
  harAnskaffelse: BooleanDefaultFalse,
  rammeavtale: RammeAvtaleEnum,
  opplaringsgruppe: z
    .string({
      required_error: "Du må velge en opplæringsgruppe",
    })
    .optional(),
  handlingsplan: HandlingsplanEnum,
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

export type HandlingsplanValue = z.infer<typeof HandlingsplanEnum>;
export type RammeavtaleValue = z.infer<typeof RammeAvtaleEnum>;
export type TiltakskodeValue = z.infer<typeof TiltakskodeEnum>;
