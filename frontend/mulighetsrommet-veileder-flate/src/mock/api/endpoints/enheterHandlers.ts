import { http, HttpResponse, PathParams } from "msw";
import { NavRegionDto } from "@api-client";
import { mockRegioner } from "@/mock/fixtures/mockRegioner";

export const enhetHandlers = [
  http.get<PathParams, NavRegionDto[]>("*/api/veilederflate/nav-enheter/regioner", () =>
    HttpResponse.json(Object.values(mockRegioner)),
  ),
];
