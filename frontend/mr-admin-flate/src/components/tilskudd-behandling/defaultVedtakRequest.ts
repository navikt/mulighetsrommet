import {
  TilskuddBehandlingRequestTilskuddRequest,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export const defaultVedtakRequest: TilskuddBehandlingRequestTilskuddRequest = {
  id: v4(),
  tilskuddOpplaeringType: null,
  soknadBelop: {
    belop: null,
    valuta: Valuta.NOK,
  },
  kommentarVedtaksbrev: null,
  vedtakResultat: null,
  utbetalingMottaker: null,
};
