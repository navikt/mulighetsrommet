import { HttpResponse, PathParams, http } from "msw";
import { PaginertUserNotifications, UserNotificationSummary } from "@mr/api-client-v2";
import { mockNotifikasjoner } from "../fixtures/mock_notifikasjoner";
import { mockUserNotificationSummary } from "../fixtures/mock_userNotificationSummary";

export const notificationHandlers = [
  http.get<PathParams, PaginertUserNotifications>("*/api/v1/intern/notifications", () =>
    HttpResponse.json(mockNotifikasjoner),
  ),
  http.get<PathParams, UserNotificationSummary>(
    "*/api/v1/intern/notifications/summary",
    () => HttpResponse.error(),
    //HttpResponse.json(mockUserNotificationSummary),
  ),
  http.post<PathParams, null>("*/api/v1/intern/notifications/status", () => HttpResponse.json()),
];
