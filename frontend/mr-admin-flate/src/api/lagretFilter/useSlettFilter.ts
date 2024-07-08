import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, LagretDokumenttype, LagretFilterService } from "mulighetsrommet-api-client";
import { QueryKeys } from "../QueryKeys";

export function useSlettFilter(dokumenttype: LagretDokumenttype) {
  const queryClient = useQueryClient();
  return useMutation<string, ApiError, string>({
    mutationFn: (id) => LagretFilterService.slettLagretFilter({ id }),
    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.lagredeFilter(dokumenttype) }),
      ]);
    },
  });
}
