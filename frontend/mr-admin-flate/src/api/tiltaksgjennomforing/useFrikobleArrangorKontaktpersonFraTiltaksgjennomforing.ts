import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { TiltaksgjennomforingerService } from "@mr/api-client";

export function useFrikobleArrangorKontaktpersonFraTiltaksgjennomforing() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (body: { kontaktpersonId: string; dokumentId: string }) => {
      return TiltaksgjennomforingerService.frikobleKontaktpersonFraTiltaksgjennomforing({
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
