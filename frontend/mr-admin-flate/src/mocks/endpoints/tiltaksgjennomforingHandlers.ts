import { HttpResponse, PathParams, http } from "msw";
import {
  Endringshistorikk,
  PaginertTiltaksgjennomforing,
  Tiltaksgjennomforing,
  TiltaksgjennomforingDeltakerSummary,
} from "mulighetsrommet-api-client";
import {
  mockTiltaksgjennomforinger,
  paginertMockTiltaksgjennomforinger,
} from "../fixtures/mock_tiltaksgjennomforinger";
import { mockEndringshistorikkForTiltaksgjennomforing } from "../fixtures/mock_endringshistorikk_tiltaksgjennomforinger";

export const tiltaksgjennomforingHandlers = [
  http.get<PathParams, PaginertTiltaksgjennomforing | { x: string }>(
    "*/api/v1/intern/tiltaksgjennomforinger",
    () => {
      return HttpResponse.json(paginertMockTiltaksgjennomforinger);
    },
  ),

  http.get<PathParams, PaginertTiltaksgjennomforing | { x: string }>(
    "*/api/v1/intern/tiltaksgjennomforinger/mine",
    () => {
      const brukerident = "B123456";
      const data = mockTiltaksgjennomforinger.filter((gj) =>
        gj.administratorer?.map((admin) => admin.navIdent).includes(brukerident),
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

  http.put<PathParams, Tiltaksgjennomforing>("*/api/v1/intern/tiltaksgjennomforinger", () => {
    const gjennomforing = mockTiltaksgjennomforinger[0];
    return HttpResponse.json({ ...gjennomforing, updatedAt: new Date().toISOString() });
  }),

  http.get<{ id: string }, Tiltaksgjennomforing | undefined>(
    "*/api/v1/intern/tiltaksgjennomforinger/skjema",
    ({ params }) => {
      const { id } = params;
      const avtale = mockTiltaksgjennomforinger.find((a) => a.id === id) ?? undefined;
      return HttpResponse.json(avtale);
    },
  ),

  http.get<PathParams, Tiltaksgjennomforing[]>(
    "*/api/v1/intern/tiltaksgjennomforinger/sok",
    ({ request }) => {
      const url = new URL(request.url);
      const tiltaksnummer = url.searchParams.get("tiltaksnummer");

      if (!tiltaksnummer) {
        throw new Error("Tiltaksnummer er ikke satt som query-param");
      }

      const gjennomforing = mockTiltaksgjennomforinger.filter((tg) =>
        (tg.tiltaksnummer ?? "").toString().includes(tiltaksnummer),
      );

      return HttpResponse.json(gjennomforing);
    },
  ),

  http.delete("/api/v1/intern/tiltaksgjennomforinger/kontaktperson", () => {
    return HttpResponse.json();
  }),

  http.get<{ id: string }, Tiltaksgjennomforing | undefined>(
    "*/api/v1/intern/tiltaksgjennomforinger/:id",
    ({ params }) => {
      const { id } = params;

      const gjennomforing = mockTiltaksgjennomforinger.find((gj) => gj.id === id);
      if (!gjennomforing) {
        return HttpResponse.json(undefined, { status: 404 });
      }

      return HttpResponse.json(gjennomforing);
    },
  ),

  http.put<{ id: string }, number>("*/api/v1/intern/tiltaksgjennomforinger/:id/avbryt", () => {
    return HttpResponse.json(1);
  }),

  http.get<{ id: string }, PaginertTiltaksgjennomforing | undefined>(
    "*/api/v1/intern/tiltaksgjennomforinger/tiltakstype/:id",
    ({ params }) => {
      const { id } = params;

      const gjennomforinger = mockTiltaksgjennomforinger.filter((gj) => gj.tiltakstype.id === id);
      if (!gjennomforinger) {
        return HttpResponse.json(undefined, { status: 404 });
      }

      return HttpResponse.json({
        pagination: {
          totalCount: gjennomforinger.length,
          pageSize: 50,
        },
        data: gjennomforinger,
      });
    },
  ),

  http.get<{ enhet: string }, PaginertTiltaksgjennomforing>(
    "*/api/v1/intern/tiltaksgjennomforinger/enhet/:enhet",
    ({ params }) => {
      const { enhet } = params;
      const gjennomforinger = mockTiltaksgjennomforinger.filter(
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

  http.get<PathParams, Endringshistorikk>(
    "*/api/v1/intern/tiltaksgjennomforinger/:id/historikk",
    () => {
      return HttpResponse.json(mockEndringshistorikkForTiltaksgjennomforing);
    },
  ),

  http.get<PathParams, TiltaksgjennomforingDeltakerSummary>(
    "*/api/v1/intern/tiltaksgjennomforinger/:id/deltaker-summary",
    () => {
      const deltakerSummary: TiltaksgjennomforingDeltakerSummary = {
        antallAktiveDeltakere: 15,
        antallAvsluttedeDeltakere: 3,
        antallDeltakereSomVenter: 10,
        antallIkkeAktuelleDeltakere: 2,
        pabegyntRegistrering: 6,
        antallDeltakere: 36,
      };
      return HttpResponse.json(deltakerSummary);
    },
  ),
];
