import { LagretDokumenttype, LagretFilterService } from "@mr/api-client";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../../QueryKeys";

interface Props {
  dokumenttype: LagretDokumenttype;
}

export function useUpdateSistBruktTimestampForLagretFilter({ dokumenttype }: Props) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => LagretFilterService.updateSistBruktTimestamp({ id }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(dokumenttype),
      });
    },
  });
}
