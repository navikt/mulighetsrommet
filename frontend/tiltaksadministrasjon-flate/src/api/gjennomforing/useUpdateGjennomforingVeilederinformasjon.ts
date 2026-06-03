import { useQueryClient } from "@tanstack/react-query";
import {
  GjennomforingService,
  GjennomforingVeilederinfoRequest,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useUpdateGjennomforingVeilederinformasjon(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, GjennomforingVeilederinfoRequest>({
    mutationFn: async (body: GjennomforingVeilederinfoRequest) => {
      return GjennomforingService.updateGjennomforingVeilederinformasjon({ path: { id }, body });
    },

    onSuccess() {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.gjennomforing(id),
      });
    },
  });
}
