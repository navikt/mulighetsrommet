import { HttpResponse, PathParams, http } from "msw";
import { NavEnhet } from "mulighetsrommet-api-client";
import { mockEnheter } from "../../fixtures/mockEnheter";

export const enhetHandlers = [
  http.get<PathParams, NavEnhet[]>("*/api/v1/internal/enheter", () =>
    HttpResponse.json(Object.values(mockEnheter)),
  ),
];
