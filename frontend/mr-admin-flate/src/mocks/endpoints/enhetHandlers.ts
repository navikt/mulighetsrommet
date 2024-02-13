import { HttpResponse, PathParams, http } from "msw";
import { NavEnhet } from "mulighetsrommet-api-client";
import { mockEnheter } from "../fixtures/mock_enheter";

export const enhetHandlers = [
  http.get<PathParams, NavEnhet[]>("*/api/v1/internal/nav-enheter", () =>
    HttpResponse.json(Object.values(mockEnheter)),
  ),
];
