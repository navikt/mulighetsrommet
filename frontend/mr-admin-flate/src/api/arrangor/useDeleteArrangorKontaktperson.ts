import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrangorService } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

export function useDeleteArrangorKontaktperson(arrangorId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn({ kontaktpersonId }: { kontaktpersonId: string }) {
      return ArrangorService.deleteArrangorKontaktperson({
        path: { id: kontaktpersonId },
      });
    },
    onSuccess() {
      queryClient.invalidateQueries({
        queryKey: QueryKeys.arrangorKontaktpersoner(arrangorId),
      });
    },
  });
}
