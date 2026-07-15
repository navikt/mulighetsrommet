import { http, HttpResponse, PathParams } from "msw";
import {
  ArrangorDto,
  ArrangorHovedenhetDto,
  ArrangorKontaktperson,
  Betalingsinformasjon,
  KoblingerForKontaktperson,
  PaginatedResponseArrangorDto,
} from "@tiltaksadministrasjon/api-client";
import { mockArrangorer, mockFretexHovedenhet } from "../fixtures/mock_arrangorer";
import { mockArrangorKontaktpersoner } from "../fixtures/mock_arrangorKontaktperson";
import { mockAvtaler } from "../fixtures/mock_avtaler";
import { mockGjennomforinger } from "../fixtures/mock_gjennomforinger";

export const arrangorHandlers = [
  http.post<PathParams, undefined, ArrangorDto | undefined>(
    "*/api/tiltaksadministrasjon/arrangorer/:orgnr",
    ({ params }) => {
      return HttpResponse.json(
        mockArrangorer.data.find((enhet) => enhet.organisasjonsnummer === params.orgnr),
      );
    },
  ),

  http.get<PathParams, undefined, PaginatedResponseArrangorDto>(
    "*/api/tiltaksadministrasjon/arrangorer",
    () => HttpResponse.json(mockArrangorer),
  ),

  http.get<PathParams, undefined, ArrangorDto | undefined>(
    "*/api/tiltaksadministrasjon/arrangorer/:id",
    ({ params }) => {
      return HttpResponse.json(mockArrangorer.data.find((enhet) => enhet.id === params.id));
    },
  ),

  http.get<PathParams, undefined, Betalingsinformasjon>(
    "*/api/tiltaksadministrasjon/arrangorer/:id/betalingsinformasjon",
    () => {
      return HttpResponse.json({ type: "BBan", kontonummer: "12345678910", kid: null });
    },
  ),

  http.get<PathParams, undefined, ArrangorHovedenhetDto | undefined>(
    "*/api/tiltaksadministrasjon/arrangorer/:id/hovedenhet",
    ({ params }) => {
      if (params.id === mockFretexHovedenhet.id) {
        return HttpResponse.json(mockFretexHovedenhet);
      }
      const enhet = mockArrangorer.data.find((enhet) => enhet.id === params.id);
      return HttpResponse.json(enhet ? { ...enhet, underenheter: [] } : undefined);
    },
  ),

  http.get<PathParams, undefined, ArrangorKontaktperson[]>(
    "*/api/tiltaksadministrasjon/arrangorer/:id/kontaktpersoner",
    () => HttpResponse.json(mockArrangorKontaktpersoner),
  ),

  http.get<PathParams, undefined, KoblingerForKontaktperson>(
    "*/api/tiltaksadministrasjon/arrangorer/kontaktperson/:id",
    () => {
      return HttpResponse.json({
        avtaler: mockAvtaler.map(({ id, navn }) => ({ id, navn })),
        gjennomforinger: mockGjennomforinger.map(({ gjennomforing: { id, navn } }) => ({
          id,
          navn,
        })),
      });
    },
  ),
];
