import { useApiQuery } from "@/hooks/useApiQuery";
import { useGetGjennomforingIdFromUrl } from "../../hooks/useGetGjennomforingIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client-v2";

export function useGjennomforingById() {
  const id = useGetGjennomforingIdFromUrl();

  return useApiQuery({
    queryKey: QueryKeys.gjennomforing(id!),
    queryFn: () =>
      GjennomforingerService.getGjennomforing({
        path: { id: id! },
      }),
    enabled: !!id,
  });
}
