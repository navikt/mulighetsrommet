import { OpprettKravVeiviserMeta, OpprettKravVeiviserSteg } from "@api-client";
import {
  gjennomforingIdAFT,
  gjennomforingIdAvklaring,
  gjennomforingIdOppfolging,
} from "./gjennomforingMocks";

const stegInvestering: OpprettKravVeiviserMeta = {
  steg: [
    { type: OpprettKravVeiviserSteg.INFORMASJON, navn: "Innsendingsinformasjon", order: 1 },
    { type: OpprettKravVeiviserSteg.UTBETALING, navn: "Utbetalingsinformasjon", order: 3 },
    { type: OpprettKravVeiviserSteg.VEDLEGG, navn: "Vedlegg", order: 4 },
    { type: OpprettKravVeiviserSteg.OPPSUMMERING, navn: "Oppsummering", order: 5 },
  ],
};

const stegAnnenAvtaltPris = stegInvestering;

const stegTimespris: OpprettKravVeiviserMeta = {
  steg: [
    { type: OpprettKravVeiviserSteg.INFORMASJON, navn: "Innsendingsinformasjon", order: 1 },
    { type: OpprettKravVeiviserSteg.DELTAKERLISTE, navn: "Deltakere", order: 2 },
    { type: OpprettKravVeiviserSteg.UTBETALING, navn: "Utbetalingsinformasjon", order: 3 },
    { type: OpprettKravVeiviserSteg.VEDLEGG, navn: "Vedlegg", order: 4 },
    { type: OpprettKravVeiviserSteg.OPPSUMMERING, navn: "Oppsummering", order: 5 },
  ],
};

export const veiviserMeta: Record<string, OpprettKravVeiviserMeta> = {
  [gjennomforingIdAFT]: stegInvestering,
  [gjennomforingIdAvklaring]: stegAnnenAvtaltPris,
  [gjennomforingIdOppfolging]: stegTimespris,
};
