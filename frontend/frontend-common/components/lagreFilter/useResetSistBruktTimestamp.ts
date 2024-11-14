import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, LagretDokumenttype, LagretFilterService } from "@mr/api-client";
import { QueryKeys } from "../../QueryKeys";

export function useResetSistBruktTimestamp(dokumenttype: LagretDokumenttype) {
  const queryClient = useQueryClient();
  return useMutation<string, ApiError, LagretDokumenttype>({
    mutationFn: (dokumenttype: LagretDokumenttype) =>
      LagretFilterService.resetSistBruktTimestampForBruker({ dokumenttype }),
    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.lagredeFilter(dokumenttype) }),
      ]);
    },
  });
}
