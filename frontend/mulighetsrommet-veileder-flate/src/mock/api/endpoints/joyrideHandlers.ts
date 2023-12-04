import { HttpResponse, PathParams, http } from "msw";
import { VeilederJoyrideRequest } from "mulighetsrommet-api-client";

export const joyrideHandlers = [
  http.post<PathParams, VeilederJoyrideRequest>("*/api/v1/internal/joyride/lagre", async () => {
    return HttpResponse.json({});
  }),
];
