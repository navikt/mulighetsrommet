import {
  TilskuddBehandlingRequestTilskuddVedtakRequest,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export function defaultVedtakRequest(): TilskuddBehandlingRequestTilskuddVedtakRequest {
  return {
    id: v4(),
    tilskuddOpplaeringType: null,
    soknadBelop: {
      belop: null,
      valuta: Valuta.NOK,
    },
    kommentarVedtaksbrev: null,
    vedtakResultat: null,
    utbetalingMottaker: null,
    kidNummer: null,
  };
}
