import { OpprettManuellUtbetalingkravRequest, UtbetalingService } from "@mr/api-client-v2";
import { ApiError } from "@mr/frontend-common/components/error-handling/errors";
import { useMutation } from "@tanstack/react-query";

export function useManueltUtbetalingskrav(kravId: string) {
  return useMutation<unknown, ApiError, OpprettManuellUtbetalingkravRequest>({
    mutationFn: (body) =>
      UtbetalingService.opprettManuellUtbetalingKrav({
        path: { id: kravId },
        body,
      }),
  });
}
