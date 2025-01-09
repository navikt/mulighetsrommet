import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../../QueryKeys";
import { LagretDokumenttype, LagretFilterRequest, LagretFilterService } from "@mr/api-client-v2";

interface Props {
  dokumenttype: LagretDokumenttype;
}

export function useLagreFilter({ dokumenttype }: Props) {
  const queryClient = useQueryClient();
  return useMutation<any, any, LagretFilterRequest>({
    mutationFn: (body) => LagretFilterService.upsertFilter({ body }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(dokumenttype),
      });
    },
  });
}
