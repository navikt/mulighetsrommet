import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { ApiError } from "mulighetsrommet-api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useDeleteTiltaksgjennomforingsnotat() {
  const client = useQueryClient();
  return useMutation<string, ApiError, string>({
    mutationFn: (id: string) => {
      return mulighetsrommetClient.tiltaksgjennomforingNotater.slettTiltaksgjennomforingNotat({
        id,
      });
    },
    async onSuccess(_, id) {
      await client.invalidateQueries({
        queryKey: QueryKeys.tiltaksgjennomforingsnotater(id),
      });
    },
  });
}
