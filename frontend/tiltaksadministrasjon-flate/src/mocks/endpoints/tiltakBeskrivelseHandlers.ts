import { http, HttpResponse, PathParams } from "msw";
import { EndringshistorikkDto } from "@tiltaksadministrasjon/api-client";
import { mockEndringshistorikkTiltakBeskrivelser } from "../fixtures/mock_endringshistorikk_tiltakbeskrivelser";

export const tiltakBeskrivelseHandlers = [
  http.get<PathParams, undefined, EndringshistorikkDto>(
    "*/api/tiltaksadministrasjon/historikk/:id",
    () => {
      return HttpResponse.json(mockEndringshistorikkTiltakBeskrivelser);
    },
  ),
];
