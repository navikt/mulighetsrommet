import { useModiaContext } from "./useModiaContext";
import { QueryKeys } from "@/api/query-keys";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { BrukerService } from "@mr/api-client-v2";

export function useBrukerdata() {
  const { fnr: norskIdent } = useModiaContext();

  return useApiSuspenseQuery({
    queryKey: QueryKeys.Bruker(norskIdent),
    queryFn: () => BrukerService.getBrukerdata({ body: { norskIdent } }),
  });
}
