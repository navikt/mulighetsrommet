import { useQuery } from "@tanstack/react-query";
import { useGetTiltakstypeIdFromUrlOrThrow } from "../../hooks/useGetTiltakstypeIdFromUrl";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";

export function useTiltakstypeById() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  return useQuery({
    queryKey: QueryKeys.tiltakstype(tiltakstypeId),
    queryFn: () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstypeById({
        id: tiltakstypeId,
      }),
    staleTime: 1000,
  });
}
