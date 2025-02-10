import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { NotificationsService } from "@mr/api-client-v2";

export function useNotificationSummary() {
  return useApiQuery({
    queryKey: QueryKeys.antallUlesteNotifikasjoner(),
    queryFn: () => NotificationsService.getNotificationSummary(),
    refetchOnWindowFocus: true,
    refetchOnMount: true,
  });
}
