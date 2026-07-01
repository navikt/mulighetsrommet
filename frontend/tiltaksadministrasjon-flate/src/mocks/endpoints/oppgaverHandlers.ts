import { http, HttpResponse, PathParams } from "msw";
import { Oppgave, OppgaveType, OppgaveTypeDto } from "@tiltaksadministrasjon/api-client";
import { mockOppgaver } from "../fixtures/mock_oppgaver";

export const oppgaverHandlers = [
  http.get<PathParams, OppgaveTypeDto[]>("*/api/tiltaksadministrasjon/oppgaver/oppgavetyper", () =>
    HttpResponse.json([
      { navn: "Tilsagn til godkjenning", type: OppgaveType.TILSAGN_TIL_GODKJENNING },
      { navn: "Utbetaling til behandling", type: OppgaveType.UTBETALING_TIL_BEHANDLING },
    ]),
  ),
  http.post<PathParams, Oppgave[]>("*/api/tiltaksadministrasjon/oppgaver", () =>
    HttpResponse.json(mockOppgaver),
  ),
];
