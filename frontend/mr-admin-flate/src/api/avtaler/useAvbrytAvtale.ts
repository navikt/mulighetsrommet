import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { ApiError } from "mulighetsrommet-api-client";
import { QueryKeys } from "../QueryKeys";

export function useAvbrytAvtale() {
  const client = useQueryClient();

  return useMutation<unknown, ApiError, string>({
    mutationFn: (id: string) => {
      return mulighetsrommetClient.avtaler.avbrytAvtale({ id });
    },
    onSuccess: (_, id) => {
      return Promise.all([
        client.invalidateQueries({
          queryKey: QueryKeys.avtale(id),
        }),

        client.invalidateQueries({
          queryKey: QueryKeys.avtaler(),
        }),
      ]);
    },
  });
}
