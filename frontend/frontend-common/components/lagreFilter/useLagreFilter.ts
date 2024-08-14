import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, LagretDokumenttype, LagretFilterRequest } from "@mr/api-client";
import { LagretFilterService } from "@mr/api-client";
import { QueryKeys } from "../../QueryKeys";

interface Props {
  dokumenttype: LagretDokumenttype;
}

export function useLagreFilter({ dokumenttype }: Props) {
  const queryClient = useQueryClient();
  return useMutation<any, ApiError, LagretFilterRequest>({
    mutationFn: (requestBody) => LagretFilterService.upsertFilter({ requestBody }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(dokumenttype),
      });
    },
  });
}
