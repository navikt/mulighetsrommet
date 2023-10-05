import { useMutation } from "@tanstack/react-query";
import { ApiError, Avtale, AvtaleRequest } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";

export function useUpsertAvtale() {
  return useMutation<Avtale, ApiError, AvtaleRequest>({
    mutationFn: (requestBody: AvtaleRequest) =>
      mulighetsrommetClient.avtaler.upsertAvtale({ requestBody }),
  });
}
