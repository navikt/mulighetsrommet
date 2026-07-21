import { http, HttpResponse, PathParams } from "msw";
import { EndringshistorikkDto } from "@tiltaksadministrasjon/api-client";
import { mockEndringshistorikkTiltakDokumenter } from "../fixtures/mock_endringshistorikk_tiltakdokumenter";

export const tiltakDokumentHandlers = [
  http.get<PathParams, undefined, EndringshistorikkDto>(
    "*/api/tiltaksadministrasjon/historikk/:id",
    () => {
      return HttpResponse.json(mockEndringshistorikkTiltakDokumenter);
    },
  ),
];
