import { HttpResponse, PathParams, http } from "msw";
import { GetKostnadsstedResponse, NavEnhet, NavRegion } from "@mr/api-client";
import { mockEnheter, mockRegioner } from "../fixtures/mock_enheter";

export const enhetHandlers = [
  http.get<PathParams, NavEnhet[]>("*/api/v1/intern/nav-enheter", () =>
    HttpResponse.json(Object.values(mockEnheter)),
  ),

  http.get<PathParams, NavEnhet[]>("*/api/v1/intern/nav-enheter/kostnadssted", () =>
    HttpResponse.json(Object.values(mockEnheter)),
  ),

  http.get<PathParams, NavRegion[]>("*/api/v1/intern/nav-enheter/regioner", () =>
    HttpResponse.json(Object.values(mockRegioner)),
  ),
];
