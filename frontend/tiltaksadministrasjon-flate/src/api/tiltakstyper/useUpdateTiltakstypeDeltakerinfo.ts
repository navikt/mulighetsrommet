import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import {
  ProblemDetail,
  TiltakstypeDeltakerinfoRequest,
  TiltakstypeDto,
  TiltakstypeService,
} from "@tiltaksadministrasjon/api-client";

export function useUpdateTiltakstypeDeltakerinfo(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: TiltakstypeDto }, ProblemDetail, TiltakstypeDeltakerinfoRequest>({
    mutationFn(body: TiltakstypeDeltakerinfoRequest) {
      return TiltakstypeService.updateDeltakerinfo({
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
