import { HttpResponse, PathParams, http } from "msw";
import {
  Bruker,
  BrukerVarsel,
  BrukerdataV2,
  GetBrukerRequest,
  HistorikkForBruker,
  Innsatsgruppe,
  NavEnhetStatus,
  NavEnhetType,
} from "mulighetsrommet-api-client";
import { historikk } from "../../fixtures/historikk";
import { historikkFraKomet } from "../../fixtures/mockHistorikkFraKomet";
import { utkastFraKomet } from "@/mock/fixtures/utkastFraKomet";

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
        erUnderOppfolging: true,
        enheter: [
          {
            navn: "NAV Sarpsborg",
            enhetsnummer: "0105",
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

  http.post<PathParams, BrukerdataV2, BrukerdataV2>("*/api/v1/internal/bruker/historikk/ny", () =>
    HttpResponse.json({ historikk: historikkFraKomet, aktive: utkastFraKomet }),
  ),
];
