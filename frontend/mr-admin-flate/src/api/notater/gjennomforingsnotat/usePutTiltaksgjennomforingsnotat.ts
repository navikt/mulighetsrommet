import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { TiltaksgjennomforingNotatRequest } from "mulighetsrommet-api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function usePutTiltaksgjennomforingsnotat() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (notat: TiltaksgjennomforingNotatRequest) => {
      return mulighetsrommetClient.tiltaksgjennomforingNotater.lagreTiltaksgjennomforingNotat({
        requestBody: notat,
      });
    },
    async onSuccess(_, request) {
      await client.invalidateQueries({
        queryKey: QueryKeys.tiltaksgjennomforingsnotater(request.id),
      });
    },
  });
}
