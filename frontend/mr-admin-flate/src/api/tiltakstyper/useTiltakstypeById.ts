import { useQuery } from "@tanstack/react-query";
import { useGetTiltakstypeIdFromUrlOrThrow } from "../../hooks/useGetTiltakstypeIdFromUrl";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

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
