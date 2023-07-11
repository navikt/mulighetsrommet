import { rest } from "msw";
import { NavEnhet } from "mulighetsrommet-api-client";
import { mockEnheter } from "../fixtures/mock_enheter";

export const enhetHandlers = [
  rest.get<any, any, NavEnhet[]>(
    "*/api/v1/internal/enheter",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockEnheter));
    }
  ),
];
