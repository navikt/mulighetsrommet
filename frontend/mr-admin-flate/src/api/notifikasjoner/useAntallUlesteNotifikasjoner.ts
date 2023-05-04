import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useAntallUlesteNotifikasjoner() {
  return useQuery(QueryKeys.antallUlesteNotifikasjoner(), () =>
    mulighetsrommetClient.notifications.getNotificationSummary()
  );
}
