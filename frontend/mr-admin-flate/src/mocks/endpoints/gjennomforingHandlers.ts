import { http, HttpResponse, PathParams } from "msw";
import { GjennomforingDeltakerSummary } from "@mr/api-client-v2";
import { mockGjennomforinger, paginertMockGjennomforinger } from "../fixtures/mock_gjennomforinger";
import { mockEndringshistorikkForGjennomforing } from "../fixtures/mock_endringshistorikk_gjennomforinger";
import {
  EndringshistorikkDto,
  GjennomforingDto,
  GjennomforingHandling,
  PaginatedResponseGjennomforingDto,
} from "@tiltaksadministrasjon/api-client";

export const gjennomforingHandlers = [
  http.get<{ id: string }, undefined, GjennomforingHandling[]>(
    "/api/tiltaksadministrasjon/gjennomforinger/:id/handlinger",
    () => {
      return HttpResponse.json([
        GjennomforingHandling.PUBLISER,
        GjennomforingHandling.REDIGER,
        GjennomforingHandling.AVBRYT,
        GjennomforingHandling.DUPLISER,
        GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING,
        GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR,
        GjennomforingHandling.OPPRETT_TILSAGN,
        GjennomforingHandling.OPPRETT_EKSTRATILSAGN,
        GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER,
        GjennomforingHandling.OPPRETT_KORREKSJON_PA_UTBETALING,
      ]);
    },
  ),

  http.get<PathParams, undefined, PaginatedResponseGjennomforingDto>(
    "*/api/tiltaksadministrasjon/gjennomforinger",
    () => {
      return HttpResponse.json(paginertMockGjennomforinger);
    },
  ),

  http.put<PathParams, undefined, GjennomforingDto>(
    "*/api/v1/intern/gjennomforinger",
    () => {
      const gjennomforing = mockGjennomforinger[0];
      return HttpResponse.json(gjennomforing);
    },
  ),

  http.get<{ id: string }, GjennomforingDto | undefined>(
    "/api/tiltaksadministrasjon/gjennomforinger/:id",
    ({ params }) => {
      const { id } = params;

      const gjennomforing = mockGjennomforinger.find((gj) => gj.id === id);
      if (!gjennomforing) {
        return HttpResponse.json(undefined, { status: 404 });
      }

      return HttpResponse.json(gjennomforing);
    },
  ),

  http.put<{ id: string }, number>("*/api/tiltaksadministrasjon/gjennomforinger/:id/avbryt", () => {
    return HttpResponse.json(1);
  }),

  http.put<{ id: string }, number>(
    "*/api/tiltaksadministrasjon/gjennomforinger/:id/tilgjengelig-for-veileder",
    () => {
      return HttpResponse.text();
    },
  ),

  http.put<{ id: string }, number>(
    "*/api/tiltaksadministrasjon/gjennomforinger/:id/apent-for-pamelding",
    () => {
      return HttpResponse.text();
    },
  ),

  http.get<PathParams, undefined, EndringshistorikkDto>(
    "*/api/tiltaksadministrasjon/gjennomforinger/:id/historikk",
    () => {
      return HttpResponse.json(mockEndringshistorikkForGjennomforing);
    },
  ),

  http.get<PathParams, GjennomforingDeltakerSummary>(
    "*/api/v1/intern/gjennomforinger/:id/deltaker-summary",
    () => {
      const deltakerSummary: GjennomforingDeltakerSummary = {
        antallDeltakere: 36,
        deltakereByStatus: [
          { status: "Deltar", count: 15 },
          { status: "Avsluttet", count: 3 },
          { status: "Venter", count: 10 },
          { status: "Ikke aktuell", count: 2 },
          { status: "Påbegynt registrering", count: 6 },
        ],
      };
      return HttpResponse.json(deltakerSummary);
    },
  ),
];
