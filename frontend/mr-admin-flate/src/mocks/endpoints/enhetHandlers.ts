import { http, HttpResponse, PathParams } from "msw";
import { NavEnhetDto, NavRegionDto } from "@tiltaksadministrasjon/api-client";
import { mockEnheter, mockRegioner } from "../fixtures/mock_enheter";

export const enhetHandlers = [
  http.get<PathParams, NavEnhetDto[]>("*/api/v1/intern/nav-enheter", () =>
    HttpResponse.json(Object.values(mockEnheter)),
  ),

  http.get<PathParams, NavEnhetDto[]>("*/api/v1/intern/nav-enheter/kostnadssted", () =>
    HttpResponse.json(Object.values(mockEnheter)),
  ),

  http.get<PathParams, NavRegionDto[]>("*/api/v1/intern/nav-enheter/regioner", () =>
    HttpResponse.json(Object.values(mockRegioner)),
  ),
];
