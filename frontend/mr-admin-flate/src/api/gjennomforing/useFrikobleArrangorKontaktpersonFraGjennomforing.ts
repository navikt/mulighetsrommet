import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { GjennomforingerService } from "@mr/api-client-v2";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useFrikobleArrangorKontaktpersonFraGjennomforing() {
  const client = useQueryClient();
  return useApiMutation({
    mutationFn: (body: { kontaktpersonId: string; dokumentId: string }) => {
      return GjennomforingerService.frikobleKontaktpersonFraGjennomforing({
        body,
      });
    },
    async onSuccess(_, request) {
      await client.invalidateQueries({
        queryKey: QueryKeys.arrangorKontaktpersonKoblinger(request.kontaktpersonId),
      });
    },
  });
}
