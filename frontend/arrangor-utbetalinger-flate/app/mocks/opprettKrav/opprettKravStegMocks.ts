import {
  OpprettKravVeiviserSteg,
  OpprettKravVeiviserStegDto,
} from "@arrangor-utbetalinger/api-client";
import { gjennomforingIdAFT, gjennomforingIdAvklaring } from "./gjennomforingMocks";

const stegInvestering: OpprettKravVeiviserStegDto[] = [
  { type: OpprettKravVeiviserSteg.INFORMASJON, navn: "Innsendingsinformasjon", order: 1 },
  { type: OpprettKravVeiviserSteg.UTBETALING, navn: "Utbetalingsinformasjon", order: 2 },
  { type: OpprettKravVeiviserSteg.VEDLEGG, navn: "Vedlegg", order: 3 },
  { type: OpprettKravVeiviserSteg.OPPSUMMERING, navn: "Oppsummering", order: 4 },
];

export const steg: Record<string, OpprettKravVeiviserStegDto[]> = {
  [gjennomforingIdAFT]: stegInvestering,
  [gjennomforingIdAvklaring]: stegInvestering,
};
