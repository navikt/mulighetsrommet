import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { ApiError } from "mulighetsrommet-api-client";

export function useAvbrytAvtale() {
  const client = useQueryClient();
  return useMutation<unknown, ApiError, string>({
    mutationFn: (id: string) => {
      return mulighetsrommetClient.avtaler.avbrytAvtale({ id });
    },
    onSuccess: () => {
      client.invalidateQueries({
        queryKey: ["avtale"],
      });
    },
  });
}
