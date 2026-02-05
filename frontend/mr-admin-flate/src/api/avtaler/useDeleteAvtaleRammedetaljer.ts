import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useDeleteRammedetaljer(avtaleId: string) {
  const queryClient = useQueryClient();

  return useApiMutation({
    mutationFn: async () => {
      return AvtaleService.deleteRammedetaljer({
        path: { id: avtaleId },
      });
    },

    onSuccess() {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.avtaleRammedetaljer(avtaleId),
      });
    },
  });
}
