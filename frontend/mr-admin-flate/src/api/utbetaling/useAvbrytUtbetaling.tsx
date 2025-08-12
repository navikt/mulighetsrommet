import { useQueryClient } from "@tanstack/react-query";
import { AarsakerOgForklaringRequest, ProblemDetail, UtbetalingService } from "@mr/api-client-v2";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useAvbrytUtbetaling() {
  const client = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string; body: AarsakerOgForklaringRequest }>({
    mutationFn: (data: { id: string; body: AarsakerOgForklaringRequest }) => {
      return UtbetalingService.avbrytUtbetaling({
        path: { id: data.id },
        body: data.body,
      });
    },
    onSuccess() {
      return Promise.all([
        client.invalidateQueries({
          queryKey: ["utbetaling"],
        }),
      ]);
    },
  });
}
