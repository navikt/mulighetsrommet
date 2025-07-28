import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "./useModiaContext";
import { DelMedBrukerService } from "@api-client";
import { useApiQuery } from "@mr/frontend-common";

export function useAlleTiltakDeltMedBruker() {
  const { fnr: norskIdent } = useModiaContext();

  return useApiQuery({
    queryKey: [...QueryKeys.AlleDeltMedBrukerStatus, norskIdent],
    queryFn: () =>
      DelMedBrukerService.getAlleTiltakDeltMedBruker<false>({
        body: { norskIdent },
      }),
    throwOnError: false, // Er ingen krise hvis dette kallet feiler
  });
}
