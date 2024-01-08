import { http, HttpResponse, PathParams } from "msw";
import { DelMedBruker, DialogResponse } from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforinger } from "../../fixtures/mockTiltaksgjennomforinger";

export const delMedBrukerHandlers = [
  http.put<PathParams, DelMedBruker, DelMedBruker>(
    "*/api/v1/internal/del-med-bruker",
    async ({ request }) => {
      const data = (await request.json()) as DelMedBruker;
      return HttpResponse.json(data);
    },
  ),

  http.post<PathParams, DelMedBruker>("*/api/v1/internal/del-med-bruker", async ({ request }) => {
    const data = (await request.json()) as DelMedBruker;
    const mockedGjennomforingId = mockTiltaksgjennomforinger[0].id;

    if (data.tiltaksgjennoforingId === mockedGjennomforingId) {
      HttpResponse.json({
        tiltaksnummer: "29518",
        navident: "V15555",
        dialogId: "12345",
        bruker_fnr: "11223344557",
        createdAt: new Date(2022, 2, 22).toString(),
      });
    } else {
      HttpResponse.json({}, { status: 204 });
    }
  }),

  http.post<PathParams, DialogResponse>("*/api/v1/internal/dialog", () =>
    HttpResponse.json({
      id: "12345",
    }),
  ),
];
