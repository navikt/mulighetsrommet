import { useQuery } from "@tanstack/react-query";
import { useGetTiltaksgjennomforingIdFromUrl } from "../../hooks/useGetTiltaksgjennomforingIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingerService } from "@mr/api-client";

export function useTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFromUrl();

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforing(id),
    queryFn: () =>
      TiltaksgjennomforingerService.getTiltaksgjennomforing({
        id: id!,
      }),
    enabled: !!id,
  });
}
