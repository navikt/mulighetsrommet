import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { GjennomforingService } from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useFrikobleArrangorKontaktpersonFraGjennomforing() {
  const client = useQueryClient();
  return useApiMutation({
    mutationFn: (body: { kontaktpersonId: string; dokumentId: string }) => {
      return GjennomforingService.frikobleGjennomforingKontaktperson({
        path: { id: body.dokumentId, kontaktpersonId: body.kontaktpersonId },
      });
    },
    async onSuccess(_, request) {
      await client.invalidateQueries({
        queryKey: QueryKeys.arrangorKontaktpersonKoblinger(request.kontaktpersonId),
      });
    },
  });
}
