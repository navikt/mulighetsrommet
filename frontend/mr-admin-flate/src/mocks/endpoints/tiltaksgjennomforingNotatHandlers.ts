import { HttpResponse, http } from "msw";
import {
  TiltaksgjennomforingNotat,
  TiltaksgjennomforingNotatRequest,
} from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforingnotater } from "../fixtures/mock_tiltaksgjennomforingnotater";

let tiltaksgjennomforingNotater = [...mockTiltaksgjennomforingnotater];

export const tiltaksgjennomforingNotatHandlers = [
  http.get<any, any, TiltaksgjennomforingNotat[]>(
    "*/api/v1/internal/notater/tiltaksgjennomforinger",
    () => HttpResponse.json(tiltaksgjennomforingNotater.sort(sortByDate)),
  ),
  http.get<any, any, TiltaksgjennomforingNotat[]>(
    "*/api/v1/internal/notater/tiltaksgjennomforinger/mine",
    () =>
      HttpResponse.json(
        tiltaksgjennomforingNotater
          .filter((notat) => notat.opprettetAv.navIdent === "B123456")
          .sort(sortByDate),
      ),
  ),

  http.put<TiltaksgjennomforingNotatRequest, any, any>(
    "*/api/v1/internal/notater/tiltaksgjennomforinger",
    async ({ request }) => {
      const payload = (await request.json()) as TiltaksgjennomforingNotatRequest;
      tiltaksgjennomforingNotater.push({
        ...payload,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        opprettetAv: {
          navIdent: "B123456",
          navn: "Bertil Bengtson",
        },
      });
      return HttpResponse.json();
    },
  ),

  http.delete<any, any, any>(
    "*/api/v1/internal/notater/tiltaksgjennomforinger/:id",
    ({ params }) => {
      const { id } = params;
      tiltaksgjennomforingNotater = [
        ...tiltaksgjennomforingNotater.filter((notat) => notat.id !== id),
      ];
      return HttpResponse.json();
    },
  ),
];

function sortByDate(a: TiltaksgjennomforingNotat, b: TiltaksgjennomforingNotat) {
  return new Date(b.createdAt).valueOf() - new Date(a.createdAt).valueOf();
}
