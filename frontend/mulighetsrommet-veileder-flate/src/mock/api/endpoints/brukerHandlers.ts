import { HttpResponse, PathParams, http } from "msw";
import {
  Bruker,
  GetBrukerRequest,
  HistorikkForBruker,
  Innsatsgruppe,
  NavEnhetType,
} from "mulighetsrommet-api-client";
import { historikk } from "../../fixtures/historikk";
import { ENHET_FREDRIKSTAD, ENHET_SARPSBORG } from "../../mock_constants";

export const brukerHandlers = [
  http.post<PathParams, GetBrukerRequest, Bruker | String>(
    "*/api/v1/internal/bruker",
    async ({ request }) => {
      const { norskIdent } = await request.json();

      if (!norskIdent) {
        return HttpResponse.json("'fnr' must be specified", { status: 400 });
      }

      const bruker: Bruker = {
        fnr: norskIdent,
        innsatsgruppe: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        oppfolgingsenhet: {
          navn: "NAV Sarpsborg",
          enhetsnummer: ENHET_SARPSBORG,
          overordnetEnhet: "0200",
          type: NavEnhetType.LOKAL,
        },
        fornavn: "IHERDIG",
        geografiskEnhet: {
          navn: "NAV Fredrikstad",
          enhetsnummer: ENHET_FREDRIKSTAD,
          overordnetEnhet: "0200",
          type: NavEnhetType.LOKAL,
        },
        manuellStatus: {
          erUnderManuellOppfolging: false,
          krrStatus: {
            kanVarsles: true,
            erReservert: false,
          },
        },
      };

      return HttpResponse.json(bruker);
    },
  ),

  http.post<PathParams, HistorikkForBruker[]>("*/api/v1/internal/bruker/historikk", () =>
    HttpResponse.json(historikk),
  ),
];
