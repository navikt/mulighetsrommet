import { HttpResponse, PathParams, http } from "msw";
import {
  Bruker,
  BrukerVarsel,
  BrukerdataFraKomet,
  GetBrukerRequest,
  HistorikkForBruker,
  Innsatsgruppe,
  NavEnhetStatus,
  NavEnhetType,
} from "mulighetsrommet-api-client";
import { historikk } from "../../fixtures/historikk";
import { historikkFraKomet, utkastFraKomet } from "../../fixtures/mockHistorikkFraKomet";
import { ENHET_SARPSBORG } from "../../mock_constants";

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
        enheter: [
          {
            navn: "NAV Sarpsborg",
            enhetsnummer: ENHET_SARPSBORG,
            overordnetEnhet: "0200",
            type: NavEnhetType.LOKAL,
            status: NavEnhetStatus.AKTIV,
          },
        ],
        varsler: [BrukerVarsel.LOKAL_OPPFOLGINGSENHET],
        fornavn: "IHERDIG",
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

  http.post<PathParams, BrukerdataFraKomet, BrukerdataFraKomet>(
    "*/api/v1/internal/bruker/historikk-fra-komet",
    () => HttpResponse.json({ historikk: historikkFraKomet, utkast: utkastFraKomet }),
  ),
];
