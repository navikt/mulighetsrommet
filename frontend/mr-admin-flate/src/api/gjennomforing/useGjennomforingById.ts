import { useQuery } from "@tanstack/react-query";
import { useGetGjennomforingIdFromUrl } from "../../hooks/useGetGjennomforingIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client";

export function useGjennomforingById() {
  const id = useGetGjennomforingIdFromUrl();

  return useQuery({
    queryKey: QueryKeys.gjennomforing(id!),
    queryFn: () =>
      GjennomforingerService.getGjennomforing({
        id: id!,
      }),
    enabled: !!id,
  });
}
