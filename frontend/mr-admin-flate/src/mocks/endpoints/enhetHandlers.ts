import { http, HttpResponse, PathParams } from "msw";
import { NavRegionDto } from "@tiltaksadministrasjon/api-client";
import { mockRegioner } from "../fixtures/mock_enheter";

export const enhetHandlers = [
  http.get<PathParams, NavRegionDto[]>(
    "*/api/tiltaksadministrasjon/nav-enheter/kostnadsstedFilter",
    () => HttpResponse.json(Object.values(mockRegioner)),
  ),

  http.get<PathParams, NavRegionDto[]>("*/api/tiltaksadministrasjon/nav-enheter/regioner", () =>
    HttpResponse.json(Object.values(mockRegioner)),
  ),
];
