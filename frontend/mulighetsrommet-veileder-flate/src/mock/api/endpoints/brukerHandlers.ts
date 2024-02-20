import { HttpResponse, PathParams, http } from "msw";
import {
  Bruker,
  BrukerVarsel,
  GetBrukerRequest,
  HistorikkForBruker,
  HistorikkForBrukerFraKomet,
  Innsatsgruppe,
  NavEnhetStatus,
  NavEnhetType,
} from "mulighetsrommet-api-client";
import { historikk } from "../../fixtures/historikk";
import { ENHET_SARPSBORG } from "../../mock_constants";
import { historikkFraKomet } from "../../fixtures/mockHistorikkFraKomet";

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
        erSykmeldtMedArbeidsgiver: false,
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

  http.post<PathParams, HistorikkForBrukerFraKomet[]>(
    "*/api/v1/internal/bruker/historikk-fra-komet",
    () => HttpResponse.json(historikkFraKomet),
  ),
];
