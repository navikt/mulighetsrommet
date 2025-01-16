import { useModiaContext } from "./useModiaContext";
import { QueryKeys } from "@/api/query-keys";
import { useSuspenseQueryWrapper } from "@/hooks/useQueryWrapper";
import { BrukerService } from "@mr/api-client-v2";

export function useHentBrukerdata() {
  const { fnr: norskIdent } = useModiaContext();

  return useSuspenseQueryWrapper({
    queryKey: QueryKeys.Bruker(norskIdent),
    queryFn: () => BrukerService.getBrukerdata({ body: { norskIdent } }),
  });
}
