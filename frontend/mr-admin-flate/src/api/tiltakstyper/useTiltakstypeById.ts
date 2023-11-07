import { useQuery } from "@tanstack/react-query";
import { useGetTiltakstypeIdFromUrl } from "../../hooks/useGetTiltakstypeIdFromUrl";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltakstypeById() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrl();

  if (!tiltakstypeId) {
    throw new Error("Fant ingen tiltakstype-id i URL");
  }

  return useQuery({
    queryKey: QueryKeys.tiltakstype(tiltakstypeId),
    queryFn: () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstypeById({
        id: tiltakstypeId!!,
      }),
    staleTime: 1000,
  });
}
