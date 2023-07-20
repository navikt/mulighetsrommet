import { rest } from "msw";
import { AvtaleNotat } from "mulighetsrommet-api-client";
import {
  mockAvtalenotater,
  mockMineAvtalenotater,
} from "../fixtures/mock_avtalenotater";

export const avtalenotatHandlers = [
  rest.get<any, any, AvtaleNotat[]>(
    "*/api/v1/internal/notater/avtaler",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockAvtalenotater));
    },
  ),
  rest.get<any, any, AvtaleNotat[]>(
    "*/api/v1/internal/notater/avtaler/mine",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockMineAvtalenotater));
    },
  ),

  rest.put<any, any, any>(
    "*/api/v1/internal/notater/avtaler/:id/",
    (req, res, ctx) => {
      return res(ctx.status(200));
    },
  ),
];
