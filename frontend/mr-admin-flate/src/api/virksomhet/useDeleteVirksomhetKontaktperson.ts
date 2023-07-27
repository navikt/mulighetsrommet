import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";

export function useDeleteVirksomhetKontaktperson() {
  return useMutation({
    mutationFn: (id: string) =>
      mulighetsrommetClient.virksomhetKontaktperson.deleteVirksomhetkontaktperson({ id }),
  });
}