import { useApiMutation } from "@/hooks/useApiMutation";
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

export function useVedtaksbrevPdfBlobPost() {
  return useApiMutation<Blob, ProblemDetail, TilskuddBehandlingRequest>({
    mutationFn: async (body) => {
      const result = await TilskuddBehandlingService.postTilskuddBehandlingVedtaksbrevPdf({ body });
      return result.data as Blob;
    },
  });
}
