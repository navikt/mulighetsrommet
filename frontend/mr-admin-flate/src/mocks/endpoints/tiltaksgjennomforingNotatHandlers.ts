import { rest } from "msw";
import {
  TiltaksgjennomforingNotat,
  TiltaksgjennomforingNotatRequest,
} from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforingnotater } from "../fixtures/mock_tiltaksgjennomforingnotater";

let tiltaksgjennomforingNotater = [...mockTiltaksgjennomforingnotater];

export const tiltaksgjennomforingNotatHandlers = [
  rest.get<any, any, TiltaksgjennomforingNotat[]>(
    "*/api/v1/internal/notater/tiltaksgjennomforinger",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(tiltaksgjennomforingNotater.sort(sortByDate)));
    },
  ),
  rest.get<any, any, TiltaksgjennomforingNotat[]>(
    "*/api/v1/internal/notater/tiltaksgjennomforinger/mine",
    (req, res, ctx) => {
      return res(
        ctx.status(200),
        ctx.json(
          tiltaksgjennomforingNotater
            .filter((notat) => notat.opprettetAv.navIdent === "B123456")
            .sort(sortByDate),
        ),
      );
    },
  ),

  rest.put<TiltaksgjennomforingNotatRequest, any, any>(
    "*/api/v1/internal/notater/tiltaksgjennomforinger",
    async (req, res, ctx) => {
      const payload = await req.json<TiltaksgjennomforingNotatRequest>();
      tiltaksgjennomforingNotater.push({
        ...payload,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        opprettetAv: {
          navIdent: "B123456",
          navn: "Bertil Betabruker",
        },
      });
      return res(ctx.status(200));
    },
  ),

  rest.delete<any, any, any>(
    "*/api/v1/internal/notater/tiltaksgjennomforinger/:id",
    (req, res, ctx) => {
      const { id } = req.params;
      tiltaksgjennomforingNotater = [
        ...tiltaksgjennomforingNotater.filter((notat) => notat.id !== id),
      ];
      return res(ctx.status(200));
    },
  ),
];

function sortByDate(a: TiltaksgjennomforingNotat, b: TiltaksgjennomforingNotat) {
  return new Date(b.createdAt).valueOf() - new Date(a.createdAt).valueOf();
}
