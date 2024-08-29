import { QueryKeys } from "@/api/query-keys";
import { useSuspenseQuery } from "@tanstack/react-query";
import { DelMedBrukerService } from "@mr/api-client";

export function useHentDeltMedBrukerStatus(norskIdent: string, gjennomforingId: string) {
  const { data: delMedBrukerInfo } = useSuspenseQuery({
    queryKey: [...QueryKeys.DeltMedBrukerStatus, norskIdent, gjennomforingId],
    queryFn: async () => {
      const result = await DelMedBrukerService.getDelMedBruker({
        requestBody: { norskIdent, tiltakId: gjennomforingId },
      });
      return result || null; // Returner null hvis API returnerer 204 No Content = undefined;
    },
  });

  return { delMedBrukerInfo };
}
