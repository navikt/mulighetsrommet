import { useMutation } from "@tanstack/react-query";
import { UtkastRequest as Utkast } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";

export function useMutateUtkast() {
  return useMutation({
    mutationFn: (data: Utkast) => mulighetsrommetClient.utkast.lagreUtkast({ requestBody: data }),
  });
}
