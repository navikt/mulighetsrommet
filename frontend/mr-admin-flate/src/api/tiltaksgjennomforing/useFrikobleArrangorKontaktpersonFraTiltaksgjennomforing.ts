import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../client";
import { QueryKeys } from "../QueryKeys";

export function useFrikobleArrangorKontaktpersonFraTiltaksgjennomforing() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (body: { kontaktpersonId: string; dokumentId: string }) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.frikobleKontaktpersonFraTiltaksgjennomforing(
        {
          requestBody: { ...body },
        },
      );
    },
    async onSuccess(_, request) {
      await client.invalidateQueries({
        queryKey: QueryKeys.arrangorKontaktpersonKoblinger(request.kontaktpersonId),
      });
    },
  });
}
