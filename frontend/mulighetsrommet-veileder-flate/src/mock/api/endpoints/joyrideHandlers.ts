import { HttpResponse, PathParams, http } from "msw";
import { JoyrideType, VeilederJoyrideRequest } from "@mr/api-client-v2";

export const joyrideHandlers = [
  http.post<PathParams, VeilederJoyrideRequest>("*/api/v1/intern/joyride/lagre", async () => {
    return HttpResponse.text("ok");
  }),

  http.get<{ type: JoyrideType }, boolean>("*/api/v1/intern/joyride/:type/har-fullfort", () => {
    return HttpResponse.json(true);
  }),
];
