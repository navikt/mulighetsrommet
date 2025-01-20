import { NotificationsService, NotificationStatus } from "@mr/api-client";

export async function notifikasjonLoader() {
  const leste = await NotificationsService.getNotifications({ status: NotificationStatus.DONE });
  const uleste = await NotificationsService.getNotifications({
    status: NotificationStatus.NOT_DONE,
  });

  return { leste, uleste };
}
