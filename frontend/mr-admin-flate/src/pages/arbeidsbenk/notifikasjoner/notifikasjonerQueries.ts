import { NotificationsService, NotificationStatus } from "@mr/api-client-v2";
import { QueryKeys } from "../../../api/QueryKeys";

export const lesteNotifikasjonerQuery = {
  queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.DONE),
  queryFn: () =>
    NotificationsService.getNotifications({ query: { status: NotificationStatus.DONE } }),
};

export const ulesteNotifikasjonerQuery = {
  queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.NOT_DONE),
  queryFn: () =>
    NotificationsService.getNotifications({
      query: { status: NotificationStatus.NOT_DONE },
    }),
};
