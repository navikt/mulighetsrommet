import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import {
  ProblemDetail,
  TiltakstypeDto,
  TiltakstypeService,
  TiltakstypeVeilederinfoRequest,
} from "@tiltaksadministrasjon/api-client";

export function useUpdateTiltakstypeVeilederinfo(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: TiltakstypeDto }, ProblemDetail, TiltakstypeVeilederinfoRequest>({
    mutationFn(body: TiltakstypeVeilederinfoRequest) {
      return TiltakstypeService.updateVeilederinfo({
        path: { id },
        body,
      });
    },
    onSuccess() {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.tiltakstype(id),
      });
    },
  });
}
