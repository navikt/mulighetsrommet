import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { NotificationStatus } from "mulighetsrommet-api-client";

export function useNotifikasjonerForAnsatt(status: NotificationStatus) {
  return useQuery(
    QueryKeys.notifikasjonerForAnsatt(status),
    () => mulighetsrommetClient.notifications.getNotifications({ status }),
    { useErrorBoundary: true },
  );
}
