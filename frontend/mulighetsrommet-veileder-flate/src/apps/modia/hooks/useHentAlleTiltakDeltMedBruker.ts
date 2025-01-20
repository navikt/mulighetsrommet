import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "./useModiaContext";
import { DelMedBrukerService } from "@mr/api-client-v2";
import { useApiQuery } from "@/hooks/useApiQuery";

export function useHentAlleTiltakDeltMedBruker() {
  const { fnr: norskIdent } = useModiaContext();

  return useApiQuery({
    queryKey: [QueryKeys.AlleDeltMedBrukerStatus, norskIdent],
    queryFn: () =>
      DelMedBrukerService.getAlleTiltakDeltMedBruker<false>({
        body: { norskIdent },
      }),
    throwOnError: false, // Er ingen krise hvis dette kallet feiler
  });
}
