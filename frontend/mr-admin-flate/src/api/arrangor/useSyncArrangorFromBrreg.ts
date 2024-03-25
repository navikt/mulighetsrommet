import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useSyncArrangorFromBrreg(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorByOrgnr(orgnr),
    queryFn: () => {
      return mulighetsrommetClient.arrangor.syncArrangorFromBrreg({ orgnr });
    },
    enabled: !!orgnr,
  });
}
