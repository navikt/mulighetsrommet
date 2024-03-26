import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";
import { NotificationStatus } from "mulighetsrommet-api-client";

export function useNotifikasjonerForAnsatt(status: NotificationStatus) {
  return useQuery({
    queryKey: QueryKeys.notifikasjonerForAnsatt(status),

    queryFn: () => mulighetsrommetClient.notifications.getNotifications({ status }),
  });
}
