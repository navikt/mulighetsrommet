import { useQuery } from "@tanstack/react-query";
import { useGetTiltaksgjennomforingIdFromUrl } from "../../hooks/useGetTiltaksgjennomforingIdFromUrl";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFromUrl();

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforing(id),
    queryFn: () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforing({
        id: id!!,
      }),
    enabled: !!id,
  });
}
