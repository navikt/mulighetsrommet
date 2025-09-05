import { http, HttpResponse, PathParams } from "msw";
import {
  PaginatedResponseUserNotification,
  UserNotificationSummary,
} from "@tiltaksadministrasjon/api-client";
import { mockNotifikasjoner } from "@/mocks/fixtures/mock_notifikasjoner";
import { mockUserNotificationSummary } from "@/mocks/fixtures/mock_userNotificationSummary";

export const notificationHandlers = [
  http.get<PathParams, PaginatedResponseUserNotification>(
    "*/api/tiltaksadministrasjon/notifications",
    () => HttpResponse.json(mockNotifikasjoner),
  ),
  http.get<PathParams, UserNotificationSummary>(
    "*/api/tiltaksadministrasjon/notifications/summary",
    () => HttpResponse.json(mockUserNotificationSummary),
  ),
  http.post<PathParams, null>("*/api/tiltaksadministrasjon/notifications/status", () =>
    HttpResponse.json(),
  ),
];
