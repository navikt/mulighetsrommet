import { HttpResponse, PathParams, http } from "msw";
import { NavVeileder } from "mulighetsrommet-api-client";

export const veilederHandlers = [
  http.get<PathParams, NavVeileder>("*/api/v1/internal/veileder/me", () =>
    HttpResponse.json({
      navIdent: "V12345",
      etternavn: "VEILEDERSEN",
      fornavn: "VEILEDER",
      hovedenhet: {
        enhetsnummer: "2990",
        navn: "Østfold",
      },
    }),
  ),
];
