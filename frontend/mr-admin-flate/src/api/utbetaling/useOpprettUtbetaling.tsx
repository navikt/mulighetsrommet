import { useMutation } from "@tanstack/react-query";
import { UtbetalingRequest, UtbetalingService } from "@mr/api-client-v2";

export function useOpprettUtbetaling(kravId: string) {
  return useMutation({
    mutationFn: (body: UtbetalingRequest) =>
      UtbetalingService.opprettUtbetaling({
        path: { id: kravId },
        body,
      }),
  });
}
