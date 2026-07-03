import { useApiMutation } from "@/hooks/useApiMutation";
import { useQuery } from "@tanstack/react-query";
import {
  ProblemDetail,
  TilskuddBehandlingRequest,
  TilskuddBehandlingService,
} from "@tiltaksadministrasjon/api-client";

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

export function useVedtaksbrevPdfBlobPost() {
  return useApiMutation<Blob, ProblemDetail, TilskuddBehandlingRequest>({
    mutationFn: async (body) => {
      const result = await TilskuddBehandlingService.postTilskuddBehandlingVedtaksbrevPdf({ body });
      return result.data as Blob;
    },
  });
}
