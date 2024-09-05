import { utkastFraKomet } from "@/mock/fixtures/mockKometUtkast";
import {
  Bruker,
  BrukerVarsel,
  DeltakerKort,
  GetAktivDeltakelseForBrukerRequest,
  GetBrukerRequest,
  GetHistorikkForBrukerRequest,
  Innsatsgruppe,
  NavEnhetStatus,
  NavEnhetType,
  TiltakshistorikkAdminDto,
} from "@mr/api-client";
import { http, HttpResponse, PathParams } from "msw";
import { historikk } from "../../fixtures/mockHistorikk";
import { historikkFraKomet } from "../../fixtures/mockKometHistorikk";

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

  http.post<PathParams, GetHistorikkForBrukerRequest, DeltakerKort[]>(
    "*/api/v1/intern/bruker/historikk",
    async ({ request }) => {
      const { type } = await request.json();
      if (type === "AKTIVE") {
        return HttpResponse.json(utkastFraKomet);
      } else {
        return HttpResponse.json(historikkFraKomet);
      }
    },
  ),
  http.post<PathParams, GetAktivDeltakelseForBrukerRequest, DeltakerKort>(
    "*/api/v1/intern/bruker/deltakelse-for-gjennomforing",
    async ({ request }) => {
      const { tiltaksgjennomforingId } = await request.json();
      const found = utkastFraKomet.find(
        (utkast) => utkast.tiltaksgjennomforingId == tiltaksgjennomforingId,
      );
      if (found) {
        return HttpResponse.json(found);
      } else {
        return HttpResponse.json(undefined, { status: 204 });
      }
    },
  ),
];
