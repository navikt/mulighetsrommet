import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { ApiError, ArrangorKontaktpersonRequest } from "mulighetsrommet-api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useDeleteArrangorKontaktperson() {
  const queryClient = useQueryClient();

  return useMutation<unknown, ApiError, { arrangorId: string; kontaktpersonId: string }>({
    mutationFn({ kontaktpersonId }) {
      return mulighetsrommetClient.arrangor.deleteArrangorKontaktperson({
        id: kontaktpersonId,
      });
    },
    onSuccess(_, { arrangorId, kontaktpersonId }) {
      queryClient.setQueryData<ArrangorKontaktpersonRequest[]>(
        QueryKeys.arrangorKontaktpersoner(arrangorId),
        (previous) => {
          return previous?.filter((kontaktperson) => kontaktperson.id !== kontaktpersonId);
        },
      );
    },
  });
}
