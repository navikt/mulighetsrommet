import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";
import { AvtaleNotatRequest } from "mulighetsrommet-api-client";

export function usePutAvtalenotat() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (notat: AvtaleNotatRequest) => {
      return mulighetsrommetClient.avtaleNotater.lagreAvtalenotat({
        requestBody: notat,
      });
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["avtalenotater"] });
    },
  });
}
