import { http, HttpResponse, PathParams } from "msw";
import { Arrangor, ArrangorKontaktperson, PaginertArrangor } from "mulighetsrommet-api-client";
import { mockArrangorKontaktpersoner } from "../fixtures/mock_arrangorKontaktperson";
import { mockArrangorer } from "../fixtures/mock_arrangorer";

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
  http.get<PathParams, ArrangorKontaktperson[]>(
    "*/api/v1/internal/arrangorer/*/kontaktpersoner",
    () => HttpResponse.json(mockArrangorKontaktpersoner),
  ),
];
