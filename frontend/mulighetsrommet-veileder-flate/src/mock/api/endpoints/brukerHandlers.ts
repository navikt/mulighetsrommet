import { http, HttpResponse, PathParams } from "msw";
import {
  Bruker,
  BrukerVarsel,
  DeltakelserResponse,
  GetBrukerRequest,
  Innsatsgruppe,
  NavEnhetStatus,
  NavEnhetType,
  TiltakshistorikkAdminDto,
} from "@mr/api-client";
import { historikk } from "../../fixtures/mockHistorikk";
import { historikkFraKomet } from "../../fixtures/mockKometHistorikk";
import { utkastFraKomet } from "@/mock/fixtures/mockKometUtkast";

export const brukerHandlers = [
  http.post<PathParams, GetBrukerRequest, Bruker | string>(
    "*/api/v1/intern/bruker",
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

  http.post<PathParams, TiltakshistorikkAdminDto[]>("*/api/v2/intern/bruker/historikk", () =>
    HttpResponse.json(historikk),
  ),

  http.post<PathParams, DeltakelserResponse, DeltakelserResponse>(
    "*/api/v1/intern/bruker/historikk",
    () => HttpResponse.json({ historiske: historikkFraKomet, aktive: utkastFraKomet }),
  ),
  http.post<PathParams, DeltakelserResponse, DeltakelserResponse>(
    "*/api/v1/intern/bruker/komet-deltakelser",
    () => HttpResponse.json({ historiske: historikkFraKomet, aktive: utkastFraKomet }),
  ),
];
