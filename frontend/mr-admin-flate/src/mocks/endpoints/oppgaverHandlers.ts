import { HttpResponse, PathParams, http } from "msw";
import { Oppgave } from "@mr/api-client-v2";
import { mockOppgaver } from "../fixtures/mock_oppgaver";

export const oppgaverHandlers = [
  http.get<PathParams, Oppgave[]>("*/api/v1/intern/oppgaver", () =>
    HttpResponse.json(mockOppgaver),
  ),
];
