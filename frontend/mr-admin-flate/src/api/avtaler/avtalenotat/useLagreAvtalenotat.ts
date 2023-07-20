import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";

export function useLagreAvtalenotat() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: () => {
      return mulighetsrommetClient.avtaleNotater.lagreAvtalenotat({});
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["avtalenotater"] });
    },
  });
}
