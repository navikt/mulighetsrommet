import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";
import { useGetTiltaksgjennomforingIdFromUrlOrThrow } from "../../../hooks/useGetTiltaksgjennomforingIdFromUrl";

export function useTiltaksgjennomforingsnotater() {
  const id = useGetTiltaksgjennomforingIdFromUrlOrThrow();

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforingsnotater(id),
    queryFn: () =>
      mulighetsrommetClient.tiltaksgjennomforingNotater.getNotaterForTiltaksgjennomforing({
        tiltaksgjennomforingId: id,
      }),
    enabled: !!id,
  });
}
