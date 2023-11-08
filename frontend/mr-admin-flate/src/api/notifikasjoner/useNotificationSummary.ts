import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useNotificationSummary() {
  return useQuery({
    queryKey: QueryKeys.antallUlesteNotifikasjoner(),
    queryFn: () => mulighetsrommetClient.notifications.getNotificationSummary(),
    refetchInterval: 1000 * 60 * 5, // Hvert 5. minutt
  });
}
