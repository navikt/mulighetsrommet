import { QueryKeys } from "@/api/query-keys";
import { useApiQuery } from "@/hooks/useApiQuery";
import { DelMedBrukerService } from "@mr/api-client-v2";

export function useHentDeltMedBrukerStatus(norskIdent: string, gjennomforingId: string) {
  const { data: delMedBrukerInfo } = useApiQuery({
    queryKey: [...QueryKeys.DeltMedBrukerStatus, norskIdent, gjennomforingId],
    queryFn: () =>
      DelMedBrukerService.getDelMedBruker({
        body: { norskIdent, tiltakId: gjennomforingId },
      }),
    throwOnError: false,
  });

  return { delMedBrukerInfo };
}
