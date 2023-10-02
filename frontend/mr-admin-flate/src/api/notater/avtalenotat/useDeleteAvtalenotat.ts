import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";
import { ApiError } from "mulighetsrommet-api-client";

export function useDeleteAvtalenotat() {
  const client = useQueryClient();
  return useMutation<string, ApiError, string>({
    mutationFn: (id: string) => {
      return mulighetsrommetClient.avtaleNotater.slettAvtalenotat({ id });
    },
    onSuccess: () => {
      client.invalidateQueries({
        queryKey: ["avtalenotater"],
      });
    },
  });
}
