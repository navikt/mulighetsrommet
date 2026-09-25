import { http, HttpResponse, PathParams } from "msw";

export const tilskuddHandlers = [
  http.get<PathParams>("*/api/tiltaksadministrasjon/tilskudd/simuler-opphor/:vedtakId", () =>
    HttpResponse.json({}),
  ),
];
