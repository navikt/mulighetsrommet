import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";

export function useAvbrytAvtale() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => {
      return mulighetsrommetClient.avtaler.avbrytAvtale({ id });
    },
    onSuccess: () => {
      client.refetchQueries({ queryKey: ["avtale"] });
    },
  });
}
