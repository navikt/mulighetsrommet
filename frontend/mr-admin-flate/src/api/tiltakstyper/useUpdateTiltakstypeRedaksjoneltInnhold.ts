import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import {
  ProblemDetail,
  TiltakstypeDto,
  TiltakstypeRedaksjoneltInnholdRequest,
  TiltakstypeService,
} from "@tiltaksadministrasjon/api-client";

export function useUpdateTiltakstypeRedaksjoneltInnhold(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<
    { data: TiltakstypeDto },
    ProblemDetail,
    TiltakstypeRedaksjoneltInnholdRequest
  >({
    mutationFn(body: TiltakstypeRedaksjoneltInnholdRequest) {
      return TiltakstypeService.upsertTiltakstypeRedaksjoneltInnhold({
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
