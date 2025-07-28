import { deltakelserAktive } from "@/mock/fixtures/mockDeltakelserAktive";
import {
  Brukerdata,
  BrukerVarsel,
  Deltakelse,
  DeltakelserMelding,
  GetAktivDeltakelseForBrukerRequest,
  GetBrukerRequest,
  GetDeltakelserForBrukerRequest,
  GetDeltakelserForBrukerResponse,
  Innsatsgruppe,
  NavEnhetType,
} from "@api-client";
import { http, HttpResponse, PathParams } from "msw";
import { deltakelserHistoriske } from "../../fixtures/mockDeltakelserHistoriske";

export const brukerHandlers = [
  http.post<PathParams, GetBrukerRequest, Brukerdata | string>(
    "*/api/veilederflate/bruker",
    async ({ request }) => {
      const { norskIdent } = await request.json();

      if (!norskIdent) {
        return HttpResponse.json("'fnr' must be specified", { status: 400 });
      }

      const bruker: Brukerdata = {
        fnr: norskIdent,
        innsatsgruppe: Innsatsgruppe.TRENGER_VEILEDNING,
        erUnderOppfolging: true,
        erSykmeldtMedArbeidsgiver: false,
        enheter: [
          {
            navn: "Nav Sarpsborg",
            enhetsnummer: "0105",
            overordnetEnhet: "0200",
            type: NavEnhetType.LOKAL,
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

  http.post<PathParams, GetDeltakelserForBrukerRequest, GetDeltakelserForBrukerResponse>(
    "*/api/veilederflate/bruker/tiltakshistorikk",
    async ({ request }) => {
      const { type } = await request.json();
      const response: GetDeltakelserForBrukerResponse = {
        meldinger: [DeltakelserMelding.MANGLER_DELTAKELSER_FRA_TEAM_TILTAK],
        deltakelser: type === "AKTIVE" ? deltakelserAktive : deltakelserHistoriske,
      };
      return HttpResponse.json(response);
    },
  ),

  http.post<PathParams, GetDeltakelserForBrukerRequest, Deltakelse[]>(
    "*/api/veilederflate/bruker/tiltakshistorikk",
    async ({ request }) => {
      const { type } = await request.json();
      if (type === "AKTIVE") {
        return HttpResponse.json(deltakelserAktive);
      } else {
        return HttpResponse.json(deltakelserHistoriske);
      }
    },
  ),

  http.post<PathParams, GetAktivDeltakelseForBrukerRequest, Deltakelse>(
    "*/api/veilederflate/bruker/deltakelse",
    async ({ request }) => {
      const { tiltakId } = await request.json();
      const found = deltakelserAktive.find((deltakelse) => {
        return "gjennomforingId" in deltakelse && deltakelse.gjennomforingId === tiltakId;
      });
      if (found) {
        return HttpResponse.json(found);
      } else {
        return HttpResponse.json(undefined, { status: 404 });
      }
    },
  ),
];
