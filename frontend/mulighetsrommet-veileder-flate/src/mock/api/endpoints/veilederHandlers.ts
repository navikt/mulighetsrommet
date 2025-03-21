import { HttpResponse, PathParams, http } from "msw";
import { NavVeileder } from "@mr/api-client-v2";

export const veilederHandlers = [
  http.get<PathParams, NavVeileder>("*/api/v1/intern/veileder/me", () =>
    HttpResponse.json({
      navIdent: "V12345",
      etternavn: "Veiledersen",
      fornavn: "Veileder",
      hovedenhet: {
        enhetsnummer: "2990",
        navn: "Østfold",
      },
    }),
  ),
];
