import { HttpResponse, PathParams, http } from "msw";
import { Oppgave, PaginertUserNotifications, UserNotificationSummary } from "@mr/api-client";
import { mockNotifikasjoner } from "../fixtures/mock_notifikasjoner";
import { mockUserNotificationSummary } from "../fixtures/mock_userNotificationSummary";
import { mockOppgaver } from "../fixtures/mock_oppgaver";

export const oppgaverHandlers = [
  http.get<PathParams, Oppgave[]>("*/api/v1/intern/oppgaver", () =>
    HttpResponse.json(mockOppgaver),
  ),
];
