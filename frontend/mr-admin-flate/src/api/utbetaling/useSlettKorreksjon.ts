import { ProblemDetail, UtbetalingService } from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSlettKorreksjon() {
  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn({ id }: { id: string }) {
      return UtbetalingService.slettKorreksjon({
        path: { id },
      });
    },
  });
}
