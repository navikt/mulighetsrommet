import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { ApiError, AvbrytAvtaleAarsak } from "mulighetsrommet-api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useAvbrytAvtale() {
  const client = useQueryClient();

  return useMutation<unknown, ApiError, { id: string; aarsak: AvbrytAvtaleAarsak | string | null }>(
    {
      mutationFn: (data: { id: string; aarsak?: AvbrytAvtaleAarsak | string | null }) => {
        return mulighetsrommetClient.avtaler.avbrytAvtale({
          id: data.id,
          requestBody: { aarsak: data.aarsak },
        });
      },
      onSuccess(_, request) {
        client.invalidateQueries({
          queryKey: QueryKeys.avtale(request.id),
        });
        client.invalidateQueries({
          queryKey: QueryKeys.avtaler(),
        });
      },
    },
  );
}
