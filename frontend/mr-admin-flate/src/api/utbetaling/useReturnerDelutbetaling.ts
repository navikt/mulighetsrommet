import {
  AarsakerOgForklaringRequestDelutbetalingReturnertAarsak,
  ProblemDetail,
  UtbetalingService,
} from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useReturnerDelutbetaling() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: AarsakerOgForklaringRequestDelutbetalingReturnertAarsak }
  >({
    mutationFn: ({ id, body }) => UtbetalingService.returnerDelutbetaling({ path: { id }, body }),
  });
}
