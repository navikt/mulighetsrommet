import { useMutation } from "@tanstack/react-query";
import { ArrangorflateService, FieldError } from "api-client";
import { queryClient } from "~/api/client";

interface GodkjennUtbetalingParams {
  id: string;
  updatedAt: string;
  kid: string | null;
}

interface GodkjennUtbetalingResult {
  success: boolean;
  errors?: FieldError[];
}

export function useGodkjennUtbetaling() {
  return useMutation({
    mutationFn: async ({
      id,
      updatedAt,
      kid,
    }: GodkjennUtbetalingParams): Promise<GodkjennUtbetalingResult> => {
      const result = await ArrangorflateService.godkjennUtbetaling({
        path: { id },
        body: { updatedAt, kid },
        client: queryClient,
      });

      if (result.error) {
        if ("errors" in result.error) {
          return { success: false, errors: result.error.errors as FieldError[] };
        }
        throw result.error;
      }

      return { success: true };
    },
  });
}
