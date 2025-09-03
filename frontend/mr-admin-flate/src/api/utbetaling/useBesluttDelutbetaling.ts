import {
  BesluttTotrinnskontrollRequestDelutbetalingReturnertAarsak,
  ProblemDetail,
  UtbetalingService,
} from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useBesluttDelutbetaling() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: BesluttTotrinnskontrollRequestDelutbetalingReturnertAarsak }
  >({
    mutationFn: ({ id, body }) => UtbetalingService.besluttDelutbetaling({ path: { id }, body }),
  });
}
