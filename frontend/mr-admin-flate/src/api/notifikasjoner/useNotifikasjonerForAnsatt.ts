import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { NotificationsService, NotificationStatus } from "mulighetsrommet-api-client";

export function useNotifikasjonerForAnsatt(status: NotificationStatus) {
  return useQuery({
    queryKey: QueryKeys.notifikasjonerForAnsatt(status),
    queryFn: () => NotificationsService.getNotifications({ status }),
  });
}
