import { useQueryClient } from "@tanstack/react-query";
import {
  GjennomforingDto,
  GjennomforingRequest,
  GjennomforingService,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useUpsertGjennomforing() {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: GjennomforingDto }, ProblemDetail, GjennomforingRequest>({
    mutationFn: async (body: GjennomforingRequest) => {
      const { data, request, response } = await GjennomforingService.upsertGjennomforing({
        body,
      });
      return { data: data as unknown as GjennomforingDto, request, response };
    },

    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforing(request.id),
        }),
      ]);
    },
  });
}
