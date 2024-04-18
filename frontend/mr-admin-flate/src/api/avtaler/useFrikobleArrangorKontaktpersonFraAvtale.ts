import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../client";
import { QueryKeys } from "../QueryKeys";

export function useFrikobleArrangorKontaktpersonFraAvtale() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (body: { kontaktpersonId: string; dokumentId: string }) => {
      return mulighetsrommetClient.avtaler.frikobleKontaktpersonFraAvtale({
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
