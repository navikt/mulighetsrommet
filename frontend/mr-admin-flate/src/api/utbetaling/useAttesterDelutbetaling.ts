import { ProblemDetail, UtbetalingService } from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useAttesterDelutbetaling() {
  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => UtbetalingService.attesterDelutbetaling({ path: { id } }),
  });
}
