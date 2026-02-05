import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrangorflateService, FieldError } from "api-client";
import { queryClient as apiClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

interface AvbrytUtbetalingParams {
  begrunnelse: string | null;
}

interface AvbrytUtbetalingResult {
  success: boolean;
  errors?: FieldError[];
}

export function useAvbrytUtbetaling(utbetalingId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      begrunnelse,
    }: AvbrytUtbetalingParams): Promise<AvbrytUtbetalingResult> => {
      const result = await ArrangorflateService.avbrytUtbetaling({
        path: { id: utbetalingId },
        body: { begrunnelse },
        client: apiClient,
      });

      if (result.error) {
        if ("errors" in result.error) {
          return { success: false, errors: result.error.errors as FieldError[] };
        }
        throw result.error;
      }

      return { success: true };
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.utbetaling(utbetalingId) });
    },
  });
}
