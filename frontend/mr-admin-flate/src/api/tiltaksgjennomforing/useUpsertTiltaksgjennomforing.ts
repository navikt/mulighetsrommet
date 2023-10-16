import { useMutation } from "@tanstack/react-query";
import {
  ApiError,
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
} from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";

export function useUpsertTiltaksgjennomforing() {
  return useMutation<Tiltaksgjennomforing, ApiError, TiltaksgjennomforingRequest>({
    mutationFn: (requestBody: TiltaksgjennomforingRequest) =>
      mulighetsrommetClient.tiltaksgjennomforinger.upsertTiltaksgjennomforing({
        requestBody,
      }),
  });
}
