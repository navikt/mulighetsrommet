import { OpprettKravUtbetalingSteg, Valuta } from "@arrangor-utbetalinger/api-client";
import {
  gjennomforingIdAFT,
  gjennomforingIdAvklaring,
  gjennomforingIdOppfolging,
} from "./gjennomforingMocks";

export const kontonummer = "10002427740";
const informasjonFelles: OpprettKravUtbetalingSteg = {
  kontonummer: kontonummer,
  valuta: Valuta.NOK,
};

export const utbetalingsInformasjon: Record<string, OpprettKravUtbetalingSteg> = {
  [gjennomforingIdAFT]: informasjonFelles,
  [gjennomforingIdAvklaring]: informasjonFelles,
  [gjennomforingIdOppfolging]: {
    ...informasjonFelles,
  },
};
