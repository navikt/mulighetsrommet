import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";

export function useDeleteUtkast() {
  return useMutation({
    mutationFn: (id: string) => mulighetsrommetClient.utkast.deleteUtkast({ id }),
  });
}
