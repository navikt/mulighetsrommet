import { useMutation } from "@tanstack/react-query";
import { RefusjonskravService, UtbetalingRequest } from "@mr/api-client-v2";

export function useOpprettUtbetaling(kravId: string) {
  return useMutation({
    mutationFn: (body: UtbetalingRequest) =>
      RefusjonskravService.opprettUtbetaling({
        path: { id: kravId },
        body,
      }),
  });
}
