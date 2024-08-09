import { useQuery } from "@tanstack/react-query";
import { useGetTiltakstypeIdFromUrlOrThrow } from "../../hooks/useGetTiltakstypeIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstyperService } from "mulighetsrommet-api-client";

export function useTiltakstypeById() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  return useQuery({
    queryKey: QueryKeys.tiltakstype(tiltakstypeId),
    queryFn: () =>
      TiltakstyperService.getTiltakstypeById({
        id: tiltakstypeId,
      }),
    staleTime: 1000,
  });
}
