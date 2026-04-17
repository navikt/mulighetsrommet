import {
  TilskuddBehandlingRequestTilskuddVedtakRequest,
  TilskuddOpplaeringType,
  Valuta,
  VedtakResultat,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export const defaultVedtakRequest: TilskuddBehandlingRequestTilskuddVedtakRequest = {
  id: v4(),
  tilskuddOpplaeringType: TilskuddOpplaeringType.SKOLEPENGER,
  soknadBelop: {
    belop: null,
    valuta: Valuta.NOK,
  },
  kommentarVedtaksbrev: null,
  vedtakResultat: VedtakResultat.INNVILGELSE,
  utbetalingMottaker: "bruker",
};
