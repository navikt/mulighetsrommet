import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useFrikobleArrangorKontaktpersonFraAvtale() {
  const client = useQueryClient();
  return useApiMutation({
    mutationFn: (body: { kontaktpersonId: string; dokumentId: string }) => {
      return AvtaleService.frikobleAvtaleKontaktperson({
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
