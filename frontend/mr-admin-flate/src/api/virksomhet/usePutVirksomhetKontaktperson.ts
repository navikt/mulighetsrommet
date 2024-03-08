import { useMutation } from "@tanstack/react-query";
import {
  ApiError,
  VirksomhetKontaktperson,
  VirksomhetKontaktpersonRequest,
} from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";

export function usePutVirksomhetKontaktperson(orgnr: string) {
  return useMutation<VirksomhetKontaktperson, ApiError, VirksomhetKontaktpersonRequest>({
    mutationFn: (requestBody: VirksomhetKontaktpersonRequest) =>
      mulighetsrommetClient.virksomhetKontaktperson.opprettVirksomhetKontaktperson({
        orgnr,
        requestBody,
      }),
  });
}
