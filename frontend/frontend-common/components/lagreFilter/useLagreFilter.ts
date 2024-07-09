import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, LagretDokumenttype, LagretFilterRequest } from "mulighetsrommet-api-client";
import { LagretFilterService } from "mulighetsrommet-api-client";
import { QueryKeys } from "../../QueryKeys";

interface Props {
  onSuccess: () => void;
  dokumenttype: LagretDokumenttype;
}

export function useLagreFilter({ onSuccess, dokumenttype }: Props) {
  const queryClient = useQueryClient();
  return useMutation<any, ApiError, LagretFilterRequest>({
    mutationFn: (requestBody) => LagretFilterService.upsertFilter({ requestBody }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(dokumenttype),
      });
      onSuccess();
    },
  });
}
