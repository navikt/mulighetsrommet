import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useNotifikasjonerForAnsatt() {
  return useQuery(QueryKeys.ansatt, () =>
    mulighetsrommetClient.notifications.getNotifications()
  );
}
