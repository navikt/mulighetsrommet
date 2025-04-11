import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { AvtalerService } from "@mr/api-client-v2";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useFrikobleArrangorKontaktpersonFraAvtale() {
  const client = useQueryClient();
  return useApiMutation({
    mutationFn: (body: { kontaktpersonId: string; dokumentId: string }) => {
      return AvtalerService.frikobleKontaktpersonFraAvtale({
        body: { ...body },
      });
    },
    async onSuccess(_, request) {
      await client.invalidateQueries({
        queryKey: QueryKeys.arrangorKontaktpersonKoblinger(request.kontaktpersonId),
      });
    },
  });
}
