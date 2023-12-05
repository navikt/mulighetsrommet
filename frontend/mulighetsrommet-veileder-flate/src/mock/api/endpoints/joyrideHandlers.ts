import { HttpResponse, PathParams, http } from "msw";
import { JoyrideType, VeilederJoyrideRequest } from "mulighetsrommet-api-client";

export const joyrideHandlers = [
  http.post<PathParams, VeilederJoyrideRequest>("*/api/v1/internal/joyride/lagre", async () => {
    return HttpResponse.text("ok");
  }),

  http.get<{ type: JoyrideType }, Boolean>("*/api/v1/internal/joyride/:type/har-fullfort", () => {
    return HttpResponse.json(true);
  }),
];
