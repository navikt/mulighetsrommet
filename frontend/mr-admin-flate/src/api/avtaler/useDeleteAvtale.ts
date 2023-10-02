import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { ApiError } from "mulighetsrommet-api-client";

export function useDeleteAvtale() {
  return useMutation<string, ApiError, string>({
    mutationFn: (avtaleId: string) => mulighetsrommetClient.avtaler.deleteAvtale({ id: avtaleId }),
  });
}
