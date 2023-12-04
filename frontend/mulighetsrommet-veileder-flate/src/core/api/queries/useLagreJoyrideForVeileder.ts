import { useMutation } from "@tanstack/react-query";
import { JoyrideType } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";

export function useLagreJoyrideForVeileder() {
  return useMutation({
    mutationFn: (data: { joyrideType: JoyrideType; fullfort: boolean }) =>
      mulighetsrommetClient.joyride.lagreJoyrideHarKjort({
        requestBody: { ...data },
      }),
  });
}
