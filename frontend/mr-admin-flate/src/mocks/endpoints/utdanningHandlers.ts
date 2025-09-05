import { http, HttpResponse } from "msw";
import { mockUtdanningsprogrammerOgUtdanninger } from "@/mocks/fixtures/mock_utdanningsprogrammer_og_utdanninger";

export const utdanningHandlers = [
  http.get("*/api/tiltaksadministrasjon/utdanninger", () => {
    return HttpResponse.json(mockUtdanningsprogrammerOgUtdanninger);
  }),
];
