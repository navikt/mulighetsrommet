import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useNotificationSummary() {
  return useQuery({
    queryKey: QueryKeys.antallUlesteNotifikasjoner(),
    queryFn: () => mulighetsrommetClient.notifications.getNotificationSummary(),
    refetchInterval: 1000 * 60 * 5, // Hvert 5. minutt
    throwOnError: false,
  });
}
