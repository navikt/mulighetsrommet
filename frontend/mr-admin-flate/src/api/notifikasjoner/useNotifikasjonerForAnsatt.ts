import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { NotificationsService, NotificationStatus } from "@mr/api-client-v2";

export function useNotifikasjonerForAnsatt(status: NotificationStatus) {
  return useApiQuery({
    queryKey: QueryKeys.notifikasjonerForAnsatt(status),
    queryFn: () => NotificationsService.getNotifications({ query: { status } }),
  });
}
