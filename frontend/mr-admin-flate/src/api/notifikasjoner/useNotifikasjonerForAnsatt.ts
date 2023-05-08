import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { Notifikasjonsstatus } from "mulighetsrommet-api-client";

export function useNotifikasjonerForAnsatt(status: Notifikasjonsstatus) {
  return useQuery(QueryKeys.ansatt, () =>
    mulighetsrommetClient.notifications.getNotifications({ status })
  );
}
