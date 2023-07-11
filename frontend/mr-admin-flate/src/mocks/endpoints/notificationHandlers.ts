import { rest } from "msw";
import {
  PaginertUserNotifications,
  UserNotificationSummary,
} from "mulighetsrommet-api-client";
import { mockNotifikasjoner } from "../fixtures/mock_notifikasjoner";
import { mockUserNotificationSummary } from "../fixtures/mock_userNotificationSummary";

export const notificationHandlers = [
  rest.get<any, any, PaginertUserNotifications>(
    "*/api/v1/internal/notifications",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockNotifikasjoner));
    }
  ),
  rest.get<any, any, UserNotificationSummary>(
    "*/api/v1/internal/notifications/summary",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockUserNotificationSummary));
    }
  ),
  rest.post<any, any, any>(
    "*/api/v1/internal/notifications/:id/status",
    (req, res, ctx) => {
      return res(ctx.status(200));
    }
  ),
];
