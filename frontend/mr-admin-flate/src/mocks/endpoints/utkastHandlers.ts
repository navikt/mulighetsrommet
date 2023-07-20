import { DefaultBodyType, PathParams, rest } from "msw";
import { Utkast } from "mulighetsrommet-api-client";
import { mockUtkast } from "../fixtures/mock_utkast";

export const utkastHandlers = [
  rest.put<DefaultBodyType, PathParams, Utkast>(
    "*/api/v1/internal/utkast",
    async (req, res, ctx) => {
      const data = await req.json<Utkast>();

      const lagretUtkastIndex = mockUtkast.findIndex((ut) => ut.id === data.id);

      let payload: Utkast = {
        ...data,
        createdAt: new Date().toDateString(),
        updatedAt: new Date().toDateString(),
      };

      if (lagretUtkastIndex > -1) {
        const lagretUtkast = mockUtkast[lagretUtkastIndex];
        payload = {
          ...payload,
          ...lagretUtkast,
          utkastData: { ...data.utkastData },
        };
        mockUtkast[lagretUtkastIndex] = payload;
      } else {
        mockUtkast.push(data);
      }

      return res(
        ctx.status(200),
        ctx.delay(),
        ctx.json<Utkast>({ ...payload }),
      );
    },
  ),

  rest.get<DefaultBodyType, PathParams, Utkast[]>(
    "*/api/v1/internal/utkast/:id",
    async (req, res, ctx) => {
      return res(ctx.status(200), ctx.delay(), ctx.json(mockUtkast));
    },
  ),
  rest.delete<DefaultBodyType, PathParams, Utkast[]>(
    "*/api/v1/internal/utkast/:id",
    async (req, res, ctx) => {
      const { id } = req.params;
      const updated = mockUtkast.filter((ut) => ut.id !== id);
      return res(ctx.status(200), ctx.delay(), ctx.json(updated));
    },
  ),
  rest.get<DefaultBodyType, PathParams, Utkast[]>(
    "*/api/v1/internal/utkast/mine",
    async (req, res, ctx) => {
      return res(ctx.status(200), ctx.delay(), ctx.json(mockUtkast));
    },
  ),
];
