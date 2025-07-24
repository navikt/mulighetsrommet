import { HttpResponse, PathParams, http } from "msw";
import { JoyrideType, VeilederJoyrideRequest } from "@api-client";

export const joyrideHandlers = [
  http.post<PathParams, VeilederJoyrideRequest>("*/api/veilederflate/joyride/lagre", async () => {
    return HttpResponse.text("ok");
  }),

  http.get<{ type: JoyrideType }, boolean>("*/api/veilederflate/joyride/:type/har-fullfort", () => {
    return HttpResponse.json(true);
  }),
];
