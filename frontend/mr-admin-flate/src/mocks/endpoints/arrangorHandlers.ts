import { http, HttpResponse, PathParams } from "msw";
import { Arrangor, ArrangorKontaktperson, PaginertArrangor } from "mulighetsrommet-api-client";
import { mockArrangorer } from "../fixtures/mock_arrangorer";
import { mockArrangorKontaktpersoner } from "../fixtures/mock_arrangorKontaktperson";
import { mockAvtaler } from "../fixtures/mock_avtaler";
import { mockTiltaksgjennomforinger } from "../fixtures/mock_tiltaksgjennomforinger";

export const arrangorHandlers = [
  http.get<PathParams, PaginertArrangor | undefined>("*/api/v1/internal/arrangorer", () =>
    HttpResponse.json(mockArrangorer),
  ),
  // TODO Route her er nok feil, skal ikke være alle-arrangorer
  http.get<PathParams, PaginertArrangor | undefined>("*/api/v1/internal/alle-arrangorer", () =>
    HttpResponse.json(mockArrangorer),
  ),
  http.get<PathParams, Arrangor | undefined>("*/api/v1/internal/arrangorer/:id", ({ params }) => {
    return HttpResponse.json(mockArrangorer.data.find((enhet) => enhet.id === params.id));
  }),
  http.post<PathParams, Arrangor | undefined>("*/api/v1/internal/arrangorer/:id", () => {
    return HttpResponse.json(mockArrangorer.data[0]);
  }),
  http.get<PathParams, Arrangor | undefined>(
    "*/api/v1/internal/arrangorer/hovedenhet/:id",
    ({ params }) => {
      return HttpResponse.json(mockArrangorer.data.find((enhet) => enhet.id === params.id));
    },
  ),
  http.post<PathParams, Arrangor | undefined>(
    "*/api/v1/internal/arrangorer/:orgnr",
    ({ params }) => {
      return HttpResponse.json(
        Object.values(mockArrangorer.data).find(
          (enhet) => enhet.organisasjonsnummer === params.orgnr,
        ),
      );
    },
  ),
  http.get<PathParams, Arrangor | undefined>(
    "*/api/v1/internal/arrangorer/kontaktperson/:id",
    () => {
      return HttpResponse.json({
        avtaler: [...mockAvtaler],
        gjennomforinger: [...mockTiltaksgjennomforinger],
      });
    },
  ),
  http.get<PathParams, ArrangorKontaktperson[]>(
    "*/api/v1/internal/arrangorer/*/kontaktpersoner",
    () => HttpResponse.json(mockArrangorKontaktpersoner),
  ),
];
