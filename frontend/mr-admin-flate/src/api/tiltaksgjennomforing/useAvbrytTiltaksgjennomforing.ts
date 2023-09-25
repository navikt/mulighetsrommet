import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";

export function useAvbrytTiltaksgjennomforing() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.avbrytTiltaksgjennomforing({ id });
    },
    onSuccess: () => {
      client.invalidateQueries({
        queryKey: ["tiltaksgjennomforing"],
      });
    },
  });
}
