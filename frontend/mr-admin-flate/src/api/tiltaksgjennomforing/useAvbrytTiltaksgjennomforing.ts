import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { ApiError, AvbrytGjennomforingAarsak } from "mulighetsrommet-api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useAvbrytTiltaksgjennomforing() {
  const client = useQueryClient();

  return useMutation<
    unknown,
    ApiError,
    { id: string; aarsak: AvbrytGjennomforingAarsak | string | null }
  >({
    mutationFn: (data: { id: string; aarsak?: AvbrytGjennomforingAarsak | string | null }) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.avbrytTiltaksgjennomforing({
        id: data.id,
        requestBody: { aarsak: data.aarsak },
      });
    },
    async onSuccess(_, request) {
      await client.invalidateQueries({
        queryKey: QueryKeys.tiltaksgjennomforing(request.id),
      });
    },
  });
}
