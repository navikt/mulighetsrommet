import { useMutation } from "@tanstack/react-query";
import { AvtaleRequest } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";

export function usePutAvtale() {
  return useMutation({
    mutationFn: (requestBody: AvtaleRequest) =>
      mulighetsrommetClient.avtaler.opprettAvtale({ requestBody }),
  });
}
