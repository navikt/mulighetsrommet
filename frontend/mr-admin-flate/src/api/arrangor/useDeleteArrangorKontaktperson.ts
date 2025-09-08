import { useQueryClient } from "@tanstack/react-query";
import { ArrangorService, ProblemDetail } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useDeleteArrangorKontaktperson(arrangorId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { kontaktpersonId: string }>({
    mutationFn({ kontaktpersonId }: { kontaktpersonId: string }) {
      return ArrangorService.deleteArrangorKontaktperson({
        path: { id: kontaktpersonId },
      });
    },
    onSuccess() {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.arrangorKontaktpersoner(arrangorId),
      });
    },
  });
}
