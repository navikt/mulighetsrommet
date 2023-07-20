import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";

export function useDeleteAvtalenotat() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => {
      return mulighetsrommetClient.avtaleNotater.slettAvtalenotat({ id });
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["avtalenotater"] });
    },
  });
}
