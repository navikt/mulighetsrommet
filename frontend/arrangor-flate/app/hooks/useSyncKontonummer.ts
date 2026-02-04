import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrangorflateService } from "api-client";
import { queryClient as apiClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

export function useSyncKontonummer(utbetalingId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const result = await ArrangorflateService.synkroniserKontonummerForUtbetaling({
        path: { id: utbetalingId },
        client: apiClient,
      });
      if (result.error) {
        throw result.error;
      }
      return result.data;
    },
    onSuccess: () => {
      // Invalidate the utbetaling query to refetch with new kontonummer
      queryClient.invalidateQueries({ queryKey: queryKeys.utbetaling(utbetalingId) });
    },
  });
}
