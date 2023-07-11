import { rest } from "msw";
import { NavAnsatt } from "mulighetsrommet-api-client";
import { mockKontaktpersoner, mockBetabruker } from "../fixtures/mock_ansatt";

export const ansattHandlers = [
  rest.get<any, any, NavAnsatt[]>(
    "*/api/v1/internal/ansatt",
    (req, res, ctx) => {
      const roller = req.url.searchParams.getAll("roller");
      return res(
        ctx.status(200),
        ctx.json(
          mockKontaktpersoner.filter((k) =>
            k.roller.every((r) => roller.includes(r))
          )
        )
      );
    }
  ),

  rest.get<any, any, NavAnsatt>(
    "*/api/v1/internal/ansatt/me",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockBetabruker));
    }
  ),
];
