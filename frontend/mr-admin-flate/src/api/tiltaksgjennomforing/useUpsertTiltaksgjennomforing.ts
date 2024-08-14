import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  Tiltaksgjennomforing,
  TiltaksgjennomforingerService,
  TiltaksgjennomforingRequest,
} from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useUpsertTiltaksgjennomforing() {
  const queryClient = useQueryClient();

  return useMutation<Tiltaksgjennomforing, ApiError, TiltaksgjennomforingRequest>({
    mutationFn: (requestBody: TiltaksgjennomforingRequest) =>
      TiltaksgjennomforingerService.upsertTiltaksgjennomforing({
        requestBody,
      }),

    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltaksgjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltaksgjennomforing(request.id),
        }),
      ]);
    },
  });
}
