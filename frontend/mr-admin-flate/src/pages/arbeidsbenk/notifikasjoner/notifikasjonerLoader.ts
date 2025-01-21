import { NotificationsService, NotificationStatus } from "@mr/api-client-v2";

export async function notifikasjonLoader() {
  const [{ data: leste }, { data: uleste }] = await Promise.all([
    NotificationsService.getNotifications({ query: { status: NotificationStatus.DONE } }),
    NotificationsService.getNotifications({
      query: { status: NotificationStatus.NOT_DONE },
    }),
  ]);

  return { leste, uleste };
}
