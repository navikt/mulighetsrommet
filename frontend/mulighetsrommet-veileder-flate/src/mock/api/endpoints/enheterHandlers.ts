import { http, HttpResponse, PathParams } from "msw";
import { Kontorstruktur } from "@api-client";
import { mockRegioner } from "@/mock/fixtures/mockRegioner";

export const enhetHandlers = [
  http.get<PathParams, Kontorstruktur[]>("*/api/veilederflate/nav-enheter/regioner", () =>
    HttpResponse.json(Object.values(mockRegioner)),
  ),
];
