import { useMutation } from "@tanstack/react-query";
import { ProblemDetail, UtbetalingService } from "@mr/api-client-v2";

export function useDeleteDelutbetaling() {
  return useMutation<unknown, ProblemDetail, string>({
    mutationFn: (id: string) => UtbetalingService.deleteDelutbetaling({ path: { id } }),
  });
}
