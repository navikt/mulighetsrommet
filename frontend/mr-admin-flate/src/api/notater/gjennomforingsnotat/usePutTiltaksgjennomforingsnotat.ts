import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";
import { TiltaksgjennomforingNotatRequest } from "mulighetsrommet-api-client";
import { QueryKeys } from "../../QueryKeys";

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
