import { OpprettKravVeiviserSteg, OpprettKravVeiviserStegDto } from "@api-client";
import {
  gjennomforingIdAFT,
  gjennomforingIdAvklaring,
  gjennomforingIdOppfolging,
} from "./gjennomforingMocks";

const stegInvestering: OpprettKravVeiviserStegDto[] = [
  { type: OpprettKravVeiviserSteg.INFORMASJON, navn: "Innsendingsinformasjon", order: 1 },
  { type: OpprettKravVeiviserSteg.UTBETALING, navn: "Utbetalingsinformasjon", order: 3 },
  { type: OpprettKravVeiviserSteg.VEDLEGG, navn: "Vedlegg", order: 4 },
  { type: OpprettKravVeiviserSteg.OPPSUMMERING, navn: "Oppsummering", order: 5 },
];

const stegAnnenAvtaltPris = stegInvestering;

const stegTimespris: OpprettKravVeiviserStegDto[] = [
  { type: OpprettKravVeiviserSteg.INFORMASJON, navn: "Innsendingsinformasjon", order: 1 },
  { type: OpprettKravVeiviserSteg.DELTAKERLISTE, navn: "Deltakere", order: 2 },
  { type: OpprettKravVeiviserSteg.UTBETALING, navn: "Utbetalingsinformasjon", order: 3 },
  { type: OpprettKravVeiviserSteg.VEDLEGG, navn: "Vedlegg", order: 4 },
  { type: OpprettKravVeiviserSteg.OPPSUMMERING, navn: "Oppsummering", order: 5 },
];

export const steg: Record<string, OpprettKravVeiviserStegDto[]> = {
  [gjennomforingIdAFT]: stegInvestering,
  [gjennomforingIdAvklaring]: stegAnnenAvtaltPris,
  [gjennomforingIdOppfolging]: stegTimespris,
};
