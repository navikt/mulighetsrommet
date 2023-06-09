import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";

export function useDeleteTiltaksgjennomforing() {
  return useMutation({
    mutationFn: (tiltaksgjennomforingId: string) =>
      mulighetsrommetClient.tiltaksgjennomforinger.deleteTiltaksgjennomforing({ id: tiltaksgjennomforingId }),
  });
}
