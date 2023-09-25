import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";

export function useDeleteAvtale() {
  return useMutation({
    mutationFn: (avtaleId: string) => mulighetsrommetClient.avtaler.deleteAvtale({ id: avtaleId }),
  });
}
