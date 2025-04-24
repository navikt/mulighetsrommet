import { BesluttDelutbetalingRequest, ProblemDetail, UtbetalingService } from "@mr/api-client-v2";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useBesluttDelutbetaling() {
  return useApiMutation<unknown, ProblemDetail, { id: string; body: BesluttDelutbetalingRequest }>({
    mutationFn: ({ id, body }: { id: string; body: BesluttDelutbetalingRequest }) =>
      UtbetalingService.besluttDelutbetaling({ path: { id }, body }),
  });
}
