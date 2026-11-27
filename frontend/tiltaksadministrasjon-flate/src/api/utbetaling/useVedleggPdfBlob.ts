import { UtbetalingService } from "@tiltaksadministrasjon/api-client";
import { useApiQuery } from "@mr/frontend-common";

export async function fetchVedleggPdfBlob(
  utbetalingId: string,
  dokumentInfoId: string,
): Promise<Blob> {
  const result = await UtbetalingService.getUtbetalingVedleggPdf({
    path: { id: utbetalingId, dokumentInfoId },
  });
  return result.data as Blob;
}

export function useVedleggForUtbetaling(utbetalingId: string) {
  return useApiQuery({
    queryKey: ["utbetalingVedlegg", utbetalingId],
    queryFn: async () => UtbetalingService.getUtbetalingVedleggData({ path: { id: utbetalingId } }),
    throwOnError: false,
  });
}
