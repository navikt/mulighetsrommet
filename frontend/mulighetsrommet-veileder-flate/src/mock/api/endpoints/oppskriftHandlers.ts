import { HttpResponse, PathParams, http } from "msw";
import { Oppskrift } from "mulighetsrommet-api-client";
import { mockOppskrifter } from "../../fixtures/mockOppskrifter";

export const oppskriftHandlers = [
  http.get<PathParams, Oppskrift[]>("*/api/v1/internal/oppskrifter/:tiltakstypeId", () => {
    return HttpResponse.json(Object.values(mockOppskrifter));
  }),
];
