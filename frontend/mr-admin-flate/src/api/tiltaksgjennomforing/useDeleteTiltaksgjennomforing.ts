import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { ApiError } from "mulighetsrommet-api-client";

export function useDeleteTiltaksgjennomforing() {
  return useMutation<string, ApiError, string>({
    mutationFn: (tiltaksgjennomforingId: string) =>
      mulighetsrommetClient.tiltaksgjennomforinger.deleteTiltaksgjennomforing({
        id: tiltaksgjennomforingId,
      }),
  });
}
