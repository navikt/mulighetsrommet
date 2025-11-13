import { OpprettKravUtbetalingsinformasjon, OpprettKravVeiviserSteg } from "@api-client";
import {
  gjennomforingIdAFT,
  gjennomforingIdAvklaring,
  gjennomforingIdOppfolging,
} from "./gjennomforingMocks";

export const kontonummer = "10002427740";
const informasjonFelles: OpprettKravUtbetalingsinformasjon = {
  kontonummer: kontonummer,
  navigering: {
    tilbake: OpprettKravVeiviserSteg.INFORMASJON,
    neste: OpprettKravVeiviserSteg.VEDLEGG,
  },
};

export const utbetalingsInformasjon: Record<string, OpprettKravUtbetalingsinformasjon> = {
  [gjennomforingIdAFT]: informasjonFelles,
  [gjennomforingIdAvklaring]: informasjonFelles,
  [gjennomforingIdOppfolging]: {
    ...informasjonFelles,
    navigering: {
      tilbake: OpprettKravVeiviserSteg.DELTAKERLISTE,
      neste: OpprettKravVeiviserSteg.VEDLEGG,
    },
  },
};
