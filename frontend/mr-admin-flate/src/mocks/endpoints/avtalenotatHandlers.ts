import { rest } from "msw";
import { AvtaleNotat, AvtaleNotatRequest } from "mulighetsrommet-api-client";
import { mockAvtalenotater } from "../fixtures/mock_avtalenotater";

let avtalenotater = [...mockAvtalenotater];

export const avtalenotatHandlers = [
  rest.get<any, any, AvtaleNotat[]>(
    "*/api/v1/internal/notater/avtaler",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(avtalenotater.sort(sortByDate)));
    },
  ),
  rest.get<any, any, AvtaleNotat[]>(
    "*/api/v1/internal/notater/avtaler/mine",
    (req, res, ctx) => {
      return res(
        ctx.status(200),
        ctx.json(
          avtalenotater
            .filter((notat) => notat.opprettetAv.navIdent === "B99876")
            .sort(sortByDate),
        ),
      );
    },
  ),

  rest.put<AvtaleNotatRequest, any, any>(
    "*/api/v1/internal/notater/avtaler",
    async (req, res, ctx) => {
      const payload = await req.json<AvtaleNotatRequest>();
      avtalenotater.push({
        ...payload,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        opprettetAv: { navIdent: "B99876", navn: "Bertil Betabruker" },
      });
      return res(ctx.status(200));
    },
  ),

  rest.delete<any, any, any>(
    "*/api/v1/internal/notater/avtaler/:id",
    (req, res, ctx) => {
      const id = req.url.searchParams.get("id");

      console.log("id", id);
      console.log("FØR", avtalenotater);
      avtalenotater = [...avtalenotater.filter((notat) => notat.id !== id)];
      console.log("ETTER", avtalenotater);
      return res(ctx.status(200));
    },
  ),
];

function sortByDate(a: AvtaleNotat, b: AvtaleNotat) {
  return new Date(b.createdAt).valueOf() - new Date(a.createdAt).valueOf();
}
