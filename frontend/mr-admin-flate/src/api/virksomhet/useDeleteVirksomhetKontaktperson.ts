import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { ApiError, VirksomhetKontaktperson } from "mulighetsrommet-api-client";
import { QueryKeys } from "../QueryKeys";

export function useDeleteVirksomhetKontaktperson() {
  const queryClient = useQueryClient();

  return useMutation<unknown, ApiError, { virksomhetId: string; kontaktpersonId: string }>({
    mutationFn({ kontaktpersonId }) {
      return mulighetsrommetClient.virksomhet.deleteVirksomhetkontaktperson({
        id: kontaktpersonId,
      });
    },
    onSuccess(_, { virksomhetId, kontaktpersonId }) {
      queryClient.setQueryData<VirksomhetKontaktperson[]>(
        QueryKeys.virksomhetKontaktpersoner(virksomhetId),
        (previous) => {
          return previous?.filter((kontaktperson) => kontaktperson.id !== kontaktpersonId);
        },
      );
    },
  });
}
