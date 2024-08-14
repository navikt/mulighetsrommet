import { HttpResponse, PathParams, http } from "msw";
import { Oppskrift } from "@mr/api-client";
import { mockOppskrifter } from "../../fixtures/mockOppskrifter";

export const oppskriftHandlers = [
  http.get<PathParams, Oppskrift[]>("*/api/v1/intern/veileder/oppskrifter/:tiltakstypeId", () => {
    return HttpResponse.json(mockOppskrifter);
  }),
];
