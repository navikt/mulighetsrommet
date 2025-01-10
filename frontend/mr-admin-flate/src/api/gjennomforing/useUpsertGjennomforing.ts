import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  TiltaksgjennomforingDto,
  TiltaksgjennomforingerService,
  TiltaksgjennomforingRequest,
} from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useUpsertGjennomforing() {
  const queryClient = useQueryClient();

  return useMutation<TiltaksgjennomforingDto, ApiError, TiltaksgjennomforingRequest>({
    mutationFn: (requestBody: TiltaksgjennomforingRequest) =>
      TiltaksgjennomforingerService.upsertTiltaksgjennomforing({
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
