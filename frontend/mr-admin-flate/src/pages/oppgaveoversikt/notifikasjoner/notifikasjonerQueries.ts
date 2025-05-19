import { NotificationsService, NotificationStatus } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

export const lesteNotifikasjonerQuery = {
  queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.READ),
  queryFn: () =>
    NotificationsService.getNotifications({ query: { status: NotificationStatus.READ } }),
};

export const ulesteNotifikasjonerQuery = {
  queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.UNREAD),
  queryFn: () =>
    NotificationsService.getNotifications({
      query: { status: NotificationStatus.UNREAD },
    }),
};
