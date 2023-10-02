import { useMutation } from "@tanstack/react-query";
import { VirksomhetKontaktpersonRequest } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";

export function usePutVirksomhetKontaktperson(orgnr: string) {
  return useMutation({
    mutationFn: (requestBody: VirksomhetKontaktpersonRequest) =>
      mulighetsrommetClient.virksomhetKontaktperson.opprettVirksomhetKontaktperson({
        orgnr,
        requestBody,
      }),
  });
}
