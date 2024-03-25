import { http, HttpResponse, PathParams } from "msw";
import { Arrangor, ArrangorKontaktperson } from "mulighetsrommet-api-client";
import { mockArrangorKontaktpersoner } from "../fixtures/mock_arrangorKontaktperson";
import { mockArrangorer } from "../fixtures/mock_arrangorer";

export const arrangorHandlers = [
  http.get<PathParams, Arrangor[] | undefined>("*/api/v1/internal/arrangorer", () =>
    HttpResponse.json(Object.values(mockArrangorer)),
  ),
  http.get<PathParams, Arrangor | undefined>("*/api/v1/internal/arrangorer/:id", ({ params }) => {
    return HttpResponse.json(Object.values(mockArrangorer).find((enhet) => enhet.id === params.id));
  }),
  http.get<PathParams, ArrangorKontaktperson[]>(
    "*/api/v1/internal/arrangorer/*/kontaktpersoner",
    () => HttpResponse.json(mockArrangorKontaktpersoner),
  ),
];
