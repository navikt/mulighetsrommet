import { TilskuddBehandlingRequestTilskuddRequest } from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export function defaultTilskuddRequest(): TilskuddBehandlingRequestTilskuddRequest {
  return {
    id: v4(),
    tilskuddOpplaeringType: null,
    belop: null,
    soknadBelop: null,
    kommentarVedtaksbrev: null,
    vedtakResultat: null,
    utbetalingMottaker: null,
    kidNummer: null,
  };
}
