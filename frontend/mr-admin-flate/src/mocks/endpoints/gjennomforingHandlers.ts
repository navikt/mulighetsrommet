import { HttpResponse, PathParams, http } from "msw";
import {
  Endringshistorikk,
  PaginertGjennomforing,
  GjennomforingDto,
  GjennomforingDeltakerSummary,
  GjennomforingHandlinger,
} from "@mr/api-client-v2";
import { mockGjennomforinger, paginertMockGjennomforinger } from "../fixtures/mock_gjennomforinger";
import { mockEndringshistorikkForGjennomforing } from "../fixtures/mock_endringshistorikk_gjennomforinger";

export const gjennomforingHandlers = [
  http.get<{ id: string }, GjennomforingHandlinger>(
    "/api/v1/intern/gjennomforinger/:id/handlinger",
    () => {
      return HttpResponse.json({
        publiser: true,
        rediger: true,
        avbryt: true,
        dupliser: true,
        endreApenForPamelding: true,
        registrerStengtHosArrangor: true,
        opprettTilsagn: true,
        opprettEkstratilsagn: true,
        opprettTilsagnForInvesteringer: true,
        opprettKorreksjonPaUtbetaling: true,
      });
    },
  ),

  http.get<PathParams, PaginertGjennomforing | { x: string }>(
    "*/api/v1/intern/gjennomforinger",
    () => {
      return HttpResponse.json(paginertMockGjennomforinger);
    },
  ),

  http.get<PathParams, PaginertGjennomforing | { x: string }>(
    "*/api/v1/intern/gjennomforinger/mine",
    () => {
      const brukerident = "B123456";
      const data = mockGjennomforinger.filter((gj) =>
        gj.administratorer.map((admin) => admin.navIdent).includes(brukerident),
      );
      return HttpResponse.json({
        pagination: {
          pageSize: 15,
          totalCount: data.length,
        },
        data,
      });
    },
  ),

  http.put<PathParams, GjennomforingDto>("*/api/v1/intern/gjennomforinger", () => {
    const gjennomforing = mockGjennomforinger[0];
    return HttpResponse.json({ ...gjennomforing, updatedAt: new Date().toISOString() });
  }),

  http.get<{ id: string }, GjennomforingDto | undefined>(
    "*/api/v1/intern/gjennomforinger/skjema",
    ({ params }) => {
      const { id } = params;
      const avtale = mockGjennomforinger.find((a) => a.id === id) ?? undefined;
      return HttpResponse.json(avtale);
    },
  ),

  http.get<PathParams, GjennomforingDto[]>("*/api/v1/intern/gjennomforinger/sok", ({ request }) => {
    const url = new URL(request.url);
    const tiltaksnummer = url.searchParams.get("tiltaksnummer");

    if (!tiltaksnummer) {
      throw new Error("Tiltaksnummer er ikke satt som query-param");
    }

    const gjennomforing = mockGjennomforinger.filter((tg) =>
      (tg.tiltaksnummer ?? "").toString().includes(tiltaksnummer),
    );

    return HttpResponse.json(gjennomforing);
  }),

  http.delete("/api/v1/intern/gjennomforinger/kontaktperson", () => {
    return HttpResponse.json();
  }),

  http.get<{ id: string }, GjennomforingDto | undefined>(
    "/api/v1/intern/gjennomforinger/:id",
    ({ params }) => {
      const { id } = params;

      const gjennomforing = mockGjennomforinger.find((gj) => gj.id === id);
      if (!gjennomforing) {
        return HttpResponse.json(undefined, { status: 404 });
      }

      return HttpResponse.json(gjennomforing);
    },
  ),

  http.put<{ id: string }, number>("*/api/v1/intern/gjennomforinger/:id/avbryt", () => {
    return HttpResponse.json(1);
  }),

  http.put<{ id: string }, number>(
    "*/api/v1/intern/gjennomforinger/:id/tilgjengelig-for-veileder",
    () => {
      return HttpResponse.text();
    },
  ),

  http.put<{ id: string }, number>(
    "*/api/v1/intern/gjennomforinger/:id/apent-for-pamelding",
    () => {
      return HttpResponse.text();
    },
  ),

  http.get<{ id: string }, PaginertGjennomforing | undefined>(
    "*/api/v1/intern/gjennomforinger/tiltakstype/:id",
    ({ params }) => {
      const { id } = params;

      const gjennomforinger = mockGjennomforinger.filter((gj) => gj.tiltakstype.id === id);

      return HttpResponse.json({
        pagination: {
          totalCount: gjennomforinger.length,
          pageSize: 50,
        },
        data: gjennomforinger,
      });
    },
  ),

  http.get<{ enhet: string }, PaginertGjennomforing>(
    "*/api/v1/intern/gjennomforinger/enhet/:enhet",
    ({ params }) => {
      const { enhet } = params;
      const gjennomforinger = mockGjennomforinger.filter(
        (gj) => gj.arenaAnsvarligEnhet?.enhetsnummer === enhet,
      );
      return HttpResponse.json({
        pagination: {
          totalCount: gjennomforinger.length,
          pageSize: 50,
        },
        data: gjennomforinger,
      });
    },
  ),

  http.get<PathParams, Endringshistorikk>("*/api/v1/intern/gjennomforinger/:id/historikk", () => {
    return HttpResponse.json(mockEndringshistorikkForGjennomforing);
  }),

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
