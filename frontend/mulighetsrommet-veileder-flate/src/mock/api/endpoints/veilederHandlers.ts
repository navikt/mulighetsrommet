import { http, HttpResponse, PathParams } from "msw";
import { NavVeilederDto } from "@api-client";

export const veilederHandlers = [
  http.get<PathParams, NavVeilederDto>("*/api/veilederflate/me", () =>
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
