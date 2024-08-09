import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { NotificationsService } from "mulighetsrommet-api-client";

export function useNotificationSummary() {
  return useQuery({
    queryKey: QueryKeys.antallUlesteNotifikasjoner(),
    queryFn: () => NotificationsService.getNotificationSummary(),
    refetchOnWindowFocus: true,
    refetchOnMount: true,
    throwOnError: false,
  });
}
