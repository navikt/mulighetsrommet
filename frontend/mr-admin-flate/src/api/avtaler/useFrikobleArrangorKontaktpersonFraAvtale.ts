import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { AvtalerService } from "@mr/api-client";

export function useFrikobleArrangorKontaktpersonFraAvtale() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (body: { kontaktpersonId: string; dokumentId: string }) => {
      return AvtalerService.frikobleKontaktpersonFraAvtale({
        requestBody: { ...body },
      });
    },
    async onSuccess(_, request) {
      await client.invalidateQueries({
        queryKey: QueryKeys.arrangorKontaktpersonKoblinger(request.kontaktpersonId),
      });
    },
  });
}
