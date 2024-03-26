import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";
import { useGetTiltaksgjennomforingIdFromUrlOrThrow } from "../../../hooks/useGetTiltaksgjennomforingIdFromUrl";

export function useMineTiltaksgjennomforingsnotater() {
  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFromUrlOrThrow();

  return useQuery({
    queryKey: QueryKeys.mineTiltaksgjennomforingsnotater(tiltaksgjennomforingId),
    queryFn: () =>
      mulighetsrommetClient.tiltaksgjennomforingNotater.getMineTiltaksgjennomforingNotater({
        tiltaksgjennomforingId,
      }),
  });
}
