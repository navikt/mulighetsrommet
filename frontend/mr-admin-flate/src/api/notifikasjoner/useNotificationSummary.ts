import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { NotificationsService } from "mulighetsrommet-api-client";

export function useNotificationSummary() {
  return useQuery({
    queryKey: QueryKeys.antallUlesteNotifikasjoner(),
    queryFn: () => NotificationsService.getNotificationSummary(),
    refetchInterval: 1000 * 60 * 5, // Hvert 5. minutt
    throwOnError: false,
  });
}
