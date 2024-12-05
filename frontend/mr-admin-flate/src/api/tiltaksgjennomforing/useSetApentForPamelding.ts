import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingerService } from "@mr/api-client";

export function useSetApentForPamelding(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (apentForPamelding: boolean) => {
      return TiltaksgjennomforingerService.setApentForPamelding({
        id: id,
        requestBody: { apentForPamelding },
      });
    },

    onSuccess() {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltaksgjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltaksgjennomforing(id),
        }),
      ]);
    },

    throwOnError: true,
  });
}
