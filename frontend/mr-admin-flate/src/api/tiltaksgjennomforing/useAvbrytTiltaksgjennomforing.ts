import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, AvbrytGjennomforingAarsak, TiltaksgjennomforingerService } from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useAvbrytTiltaksgjennomforing() {
  const client = useQueryClient();

  return useMutation<unknown, ApiError, { id: string; aarsak: AvbrytGjennomforingAarsak | string }>(
    {
      mutationFn: (data: { id: string; aarsak?: AvbrytGjennomforingAarsak | string }) => {
        return TiltaksgjennomforingerService.avbrytTiltaksgjennomforing({
          id: data.id,
          requestBody: { aarsak: data.aarsak },
        });
      },
      onSuccess(_, request) {
        return Promise.all([
          client.invalidateQueries({
            queryKey: QueryKeys.tiltaksgjennomforing(request.id),
          }),
          client.invalidateQueries({
            queryKey: QueryKeys.tiltaksgjennomforinger(),
          }),
        ]);
      },
    },
  );
}
