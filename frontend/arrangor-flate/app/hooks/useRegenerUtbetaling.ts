import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrangorflateService, FieldError } from "api-client";
import { queryClient as apiClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

interface RegenerUtbetalingResult {
  success: boolean;
  errors?: FieldError[];
}

export function useRegenerUtbetaling(utbetalingId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (): Promise<RegenerUtbetalingResult> => {
      const result = await ArrangorflateService.regenererUtbetaling({
        path: { id: utbetalingId },
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
      // Invalidate the utbetaling query to refetch with updated status
      queryClient.invalidateQueries({ queryKey: queryKeys.utbetaling(utbetalingId) });
    },
  });
}
