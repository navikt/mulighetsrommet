import {
  BesluttTotrinnskontrollRequest,
  ProblemDetail,
  UtbetalingService,
} from "@mr/api-client-v2";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useBesluttDelutbetaling() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: BesluttTotrinnskontrollRequest }
  >({
    mutationFn: ({ id, body }: { id: string; body: BesluttTotrinnskontrollRequest }) =>
      UtbetalingService.besluttDelutbetaling({ path: { id }, body }),
  });
}
