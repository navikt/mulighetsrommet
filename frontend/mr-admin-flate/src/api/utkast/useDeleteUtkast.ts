import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { ApiError } from "mulighetsrommet-api-client";

export function useDeleteUtkast() {
  return useMutation<string, ApiError, string>({
    mutationFn: (id: string) => mulighetsrommetClient.utkast.deleteUtkast({ id }),
  });
}
