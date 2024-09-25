import { http, HttpResponse } from "msw";
import { mockProgramomraderOgUtdanninger } from "../fixtures/mock_programomrader_og_utdanninger";

export const utdanningHandlers = [
  http.get("*/api/v1/intern/utdanninger", () => {
    return HttpResponse.json(mockProgramomraderOgUtdanninger);
  }),
];
