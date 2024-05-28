import { HttpResponse, PathParams, http } from "msw";
import { PaginertUserNotifications, UserNotificationSummary } from "mulighetsrommet-api-client";
import { mockNotifikasjoner } from "../fixtures/mock_notifikasjoner";
import { mockUserNotificationSummary } from "../fixtures/mock_userNotificationSummary";

export const notificationHandlers = [
  http.get<PathParams, PaginertUserNotifications>("*/api/v1/intern/notifications", () =>
    HttpResponse.json(mockNotifikasjoner),
  ),
  http.get<PathParams, UserNotificationSummary>("*/api/v1/intern/notifications/summary", () =>
    HttpResponse.json(mockUserNotificationSummary),
  ),
  http.post<PathParams, null>("*/api/v1/intern/notifications/:id/status", () =>
    HttpResponse.json(),
  ),
];
