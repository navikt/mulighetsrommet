import { useQuery } from "@tanstack/react-query";
import { TilskuddBehandlingService } from "@tiltaksadministrasjon/api-client";

export async function fetchVedtaksbrevPdfBlob(behandlingId: string): Promise<Blob> {
  const result = await TilskuddBehandlingService.getTilskuddBehandlingVedtaksbrevPdf({
    path: { id: behandlingId },
  });
  return result.data as Blob;
}

export function useVedtaksbrevPdfBlob(behandlingId: string, enabled: boolean) {
  return useQuery({
    queryKey: ["vedtaksbrev-pdf", behandlingId],
    queryFn: () => fetchVedtaksbrevPdfBlob(behandlingId),
    enabled,
    staleTime: Infinity,
    gcTime: 0,
  });
}
