import { DefaultBodyType, PathParams, rest } from "msw";
import {
  PaginertUserNotifications,
  UserNotificationSummary,
} from "mulighetsrommet-api-client";
import { mockNotifikasjoner } from "../fixtures/mock_notifikasjoner";
import { mockUserNotificationSummary } from "../fixtures/mock_userNotificationSummary";

export const notificationHandlers = [
  rest.get<DefaultBodyType, PathParams, PaginertUserNotifications>(
    "*/api/v1/internal/notifications",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockNotifikasjoner));
    },
  ),
  rest.get<DefaultBodyType, PathParams, UserNotificationSummary>(
    "*/api/v1/internal/notifications/summary",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockUserNotificationSummary));
    },
  ),
  rest.post<DefaultBodyType, PathParams, null>(
    "*/api/v1/internal/notifications/:id/status",
    (req, res, ctx) => {
      return res(ctx.status(200));
    },
  ),
];
