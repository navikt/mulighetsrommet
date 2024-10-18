import { http, HttpResponse } from "msw";
import { mockUtdanningsprogrammerOgUtdanninger } from "../fixtures/mock_utdanningsprogrammer_og_utdanninger";

export const utdanningHandlers = [
  http.get("*/api/v1/intern/utdanninger", () => {
    return HttpResponse.json(mockUtdanningsprogrammerOgUtdanninger);
  }),
];
