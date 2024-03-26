import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { ApiError } from "mulighetsrommet-api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useAvbrytTiltaksgjennomforing() {
  const client = useQueryClient();
  return useMutation<string, ApiError, string>({
    mutationFn: (id: string) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.avbrytTiltaksgjennomforing({ id });
    },
    async onSuccess() {
      await client.invalidateQueries({
        queryKey: QueryKeys.tiltaksgjennomforing(),
      });
    },
  });
}
