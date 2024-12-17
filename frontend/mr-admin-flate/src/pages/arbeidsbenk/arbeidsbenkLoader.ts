import { NotificationsService, NotificationStatus } from "@mr/api-client";

export async function arbeidsbenkLoader() {
  const leste = await NotificationsService.getNotifications({ status: NotificationStatus.DONE });
  const uleste = await NotificationsService.getNotifications({
    status: NotificationStatus.NOT_DONE,
  });

  return {
    notifikasjoner: leste?.pagination.totalCount + uleste?.pagination.totalCount,
  };
}
