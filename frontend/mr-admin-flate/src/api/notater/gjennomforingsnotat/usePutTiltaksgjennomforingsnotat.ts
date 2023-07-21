import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";
import { TiltaksgjennomforingNotatRequest } from "mulighetsrommet-api-client";

export function usePutTiltaksgjennomforingsnotat() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (notat: TiltaksgjennomforingNotatRequest) => {
      return mulighetsrommetClient.tiltaksgjennomforingNotater.lagreTiltaksgjennomforingNotat(
        {
          requestBody: notat,
        },
      );
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["tiltaksgjennomforingsnotater"] });
    },
  });
}
