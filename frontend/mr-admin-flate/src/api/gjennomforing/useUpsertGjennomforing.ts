import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  GjennomforingDto,
  GjennomforingerService,
  GjennomforingRequest,
} from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useUpsertGjennomforing() {
  const queryClient = useQueryClient();

  return useMutation<GjennomforingDto, ApiError, GjennomforingRequest>({
    mutationFn: (requestBody: GjennomforingRequest) =>
      GjennomforingerService.upsertGjennomforing({
        requestBody,
      }),

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
