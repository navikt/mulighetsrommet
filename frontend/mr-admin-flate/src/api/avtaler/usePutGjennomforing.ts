import { useMutation } from "@tanstack/react-query";
import { TiltaksgjennomforingRequest } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";

export function usePutGjennomforing() {
  return useMutation({
    mutationFn: (requestBody: TiltaksgjennomforingRequest) =>
      mulighetsrommetClient.tiltaksgjennomforinger.opprettTiltaksgjennomforing({
        requestBody,
      }),
  });
}
