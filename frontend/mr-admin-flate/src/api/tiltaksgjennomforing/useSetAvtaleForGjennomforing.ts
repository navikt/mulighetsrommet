import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";

export function useSetAvtaleForGjennomforing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { gjennomforingId: string; avtaleId?: string }) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.setAvtaleForGjennomforing({
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
