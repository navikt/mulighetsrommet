import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useParams } from "react-router";

export function useAdminGjennomforingById() {
  const { gjennomforingId } = useParams();

  return useApiSuspenseQuery({
    queryKey: QueryKeys.gjennomforing(gjennomforingId),
    queryFn: () => GjennomforingerService.getGjennomforing({ path: { id: gjennomforingId! } }),
    enabled: !!gjennomforingId,
  });
}
