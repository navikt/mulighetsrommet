import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { ApiError } from "mulighetsrommet-api-client";

export function useAvbrytTiltaksgjennomforing() {
  const client = useQueryClient();
  return useMutation<string, ApiError, string>({
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
