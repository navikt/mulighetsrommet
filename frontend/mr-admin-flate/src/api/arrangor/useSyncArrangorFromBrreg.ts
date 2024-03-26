import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useSyncArrangorFromBrreg(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorByOrgnr(orgnr),
    queryFn: () => {
      return mulighetsrommetClient.arrangor.syncArrangorFromBrreg({ orgnr });
    },
    enabled: !!orgnr,
  });
}
