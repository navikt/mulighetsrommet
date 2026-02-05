import { useMutation } from "@tanstack/react-query";
import { ArrangorflateService } from "api-client";
import { queryClient } from "~/api/client";

interface DownloadOptions {
  filename?: string;
}

export function useDownloadUtbetalingPdf(utbetalingId: string) {
  return useMutation({
    mutationFn: async (options?: DownloadOptions) => {
      const result = await ArrangorflateService.getUtbetalingPdf({
        path: { id: utbetalingId },
        client: queryClient,
      });

      if (result.error) {
        throw result.error;
      }

      // Create blob and trigger download
      const blob = result.data as Blob;
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = options?.filename ?? "kvittering.pdf";
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      return result.data;
    },
  });
}
