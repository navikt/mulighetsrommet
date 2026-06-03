import { http, HttpResponse, PathParams } from "msw";
import { Oppgave } from "@tiltaksadministrasjon/api-client";
import { mockOppgaver } from "../fixtures/mock_oppgaver";

export const oppgaverHandlers = [
  http.post<PathParams, Oppgave[]>("*/api/tiltaksadministrasjon/oppgaver", () =>
    HttpResponse.json(mockOppgaver),
  ),
];
