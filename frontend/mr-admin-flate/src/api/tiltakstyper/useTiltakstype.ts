import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";

export function useTiltakstype(id?: string) {
  return useQuery({
    queryKey: QueryKeys.tiltakstype(id),
    queryFn: () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstypeById({
        id: id!,
      }),
    staleTime: 1000,
    enabled: !!id,
  });
}
