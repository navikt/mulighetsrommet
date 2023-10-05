import { useMutation } from "@tanstack/react-query";
import {
  ApiError,
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
} from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";

export function usePutGjennomforing() {
  return useMutation<Tiltaksgjennomforing, ApiError, TiltaksgjennomforingRequest>({
    mutationFn: (requestBody: TiltaksgjennomforingRequest) =>
      mulighetsrommetClient.tiltaksgjennomforinger.opprettTiltaksgjennomforing({
        requestBody,
      }),
  });
}
