import { HttpResponse, PathParams, http } from "msw";
import {
  Bruker,
  GetBrukerRequest,
  HistorikkForBruker,
  Innsatsgruppe,
} from "mulighetsrommet-api-client";
import { historikk } from "../../fixtures/historikk";
import { ENHET_FREDRIKSTAD } from "../../mock_constants";

export const brukerHandlers = [
  http.post<PathParams, GetBrukerRequest, Bruker | String>(
    "*/api/v1/internal/bruker",
    async ({ request }) => {
      const { norskIdent } = await request.json();

      if (!norskIdent) {
        return HttpResponse.json("'fnr' must be specified", { status: 400 });
      }

      return HttpResponse.json({
        fnr: norskIdent,
        innsatsgruppe: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        oppfolgingsenhet: {
          navn: "NAV Fredrikstad",
          enhetId: ENHET_FREDRIKSTAD,
        },
        fornavn: "IHERDIG",
        geografiskEnhet: {
          navn: "NAV Fredrikstad",
          enhetsnummer: ENHET_FREDRIKSTAD,
        },
        manuellStatus: {
          erUnderManuellOppfolging: false,
          krrStatus: {
            kanVarsles: true,
            erReservert: false,
          },
        },
      });
    },
  ),

  http.post<PathParams, HistorikkForBruker[]>("*/api/v1/internal/bruker/historikk", () =>
    HttpResponse.json(historikk),
  ),
];
