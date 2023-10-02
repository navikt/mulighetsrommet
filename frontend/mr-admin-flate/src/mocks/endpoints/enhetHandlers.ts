import { DefaultBodyType, PathParams, rest } from "msw";
import { NavEnhet } from "mulighetsrommet-api-client";
import { mockEnheter } from "../fixtures/mock_enheter";

export const enhetHandlers = [
  rest.get<DefaultBodyType, PathParams, NavEnhet[]>("*/api/v1/internal/enheter", (req, res, ctx) =>
    res(ctx.status(200), ctx.json(Object.values(mockEnheter))),
  ),
];
