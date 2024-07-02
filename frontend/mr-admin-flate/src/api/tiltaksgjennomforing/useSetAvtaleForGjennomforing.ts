import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingerService } from "mulighetsrommet-api-client";

export function useSetAvtaleForGjennomforing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { gjennomforingId: string; avtaleId?: string }) => {
      return TiltaksgjennomforingerService.setAvtaleForGjennomforing({
        id: data.gjennomforingId,
        requestBody: {
          avtaleId: data.avtaleId,
        },
      });
    },

    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltaksgjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltaksgjennomforing(request.gjennomforingId),
        }),
      ]);
    },
  });
}
