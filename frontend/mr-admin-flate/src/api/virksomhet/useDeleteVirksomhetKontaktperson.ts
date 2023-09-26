import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { ApiError } from "mulighetsrommet-api-client";

export function useDeleteVirksomhetKontaktperson() {
  return useMutation<unknown, ApiError, string>({
    mutationFn: (id: string) =>
      mulighetsrommetClient.virksomhetKontaktperson.deleteVirksomhetkontaktperson({ id }),
  });
}
