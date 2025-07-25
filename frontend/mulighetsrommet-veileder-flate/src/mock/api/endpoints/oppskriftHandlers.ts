import { HttpResponse, PathParams, http } from "msw";
import { Oppskrift } from "@api-client";
import { mockOppskrifter } from "../../fixtures/mockOppskrifter";

export const oppskriftHandlers = [
  http.get<PathParams, Oppskrift[]>("*/api/veilederflate/oppskrifter/:tiltakstypeId", () => {
    return HttpResponse.json(mockOppskrifter);
  }),
];
