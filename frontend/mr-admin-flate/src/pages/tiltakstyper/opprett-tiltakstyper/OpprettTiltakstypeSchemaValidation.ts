import { z } from "zod";
import { BooleanDefaultFalse } from "../../OpprettComponents";
const TiltaksgruppekodeEnum = z.enum(
  [
    "AFT",
    "AMB",
    "ARBPRAKS",
    "ARBRREHAB",
    "ARBTREN",
    "AVKLARING",
    "BEHPSSAM",
    "ETAB",
    "FORSOK",
    "LONNTILS",
    "OPPFOLG",
    "OPPL",
    "TILRETTE",
    "UTFAS",
    "VARIGASV",
  ],
  { required_error: "Du må velge en tiltaksgruppekode" }
);

const HandlingsplanEnum = z.enum(["SOK", "LAG", "TIL"], {
  required_error: "Du må velge en handlingsplan",
});
const RammeAvtaleEnum = z.enum(["SKAL", "KAN", "IKKE"], {
  required_error: "Du må velge en om tiltaket krever en rammeavtale",
});

const AdministrasjonskodeEnum = z.enum(["AMO", "IND", "INST"], {
  required_error: "Du må sette en administrasjonskode",
});

const OpplaeringsgruppeEnum = z.enum(["AMO", "UTD"], {
  required_error: "Du må gjøre et valg for opplæringsgruppe",
});

export const OpprettTiltakstypeSchema = z.object({
  tiltakstypenavn: z.string({ required_error: "Tiltakstypen må ha et navn" }),
  fraDato: z.string({
    required_error: "Du må sette en fra-dato for tiltakstypen",
  }),
  tilDato: z.string({
    required_error: "Du må sette en til-dato for tiltakstypen",
  }),
  tiltaksgruppekode: TiltaksgruppekodeEnum,
  tiltakskode: z.string({ required_error: "Du må skrive inn tiltakskode" }),
  rettTilTiltakspenger: BooleanDefaultFalse,
  administrasjonskode: AdministrasjonskodeEnum,
  kopiAvTilsagnsbrev: BooleanDefaultFalse,
  harAnskaffelse: BooleanDefaultFalse,
  rammeavtale: RammeAvtaleEnum.optional(),
  opplaringsgruppe: OpplaeringsgruppeEnum,
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

export type TiltaksgruppekodeValue = z.infer<typeof TiltaksgruppekodeEnum>;
export type HandlingsplanValue = z.infer<typeof HandlingsplanEnum>;
export type RammeavtaleValue = z.infer<typeof RammeAvtaleEnum>;
export type AdministrasjonskodeValue = z.infer<typeof AdministrasjonskodeEnum>;
export type OpplaeringsgruppeValue = z.infer<typeof OpplaeringsgruppeEnum>;
