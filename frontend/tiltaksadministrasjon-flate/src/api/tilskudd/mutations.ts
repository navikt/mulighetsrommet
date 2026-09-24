import { client, ProblemDetail } from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSimulerOpphorTilskuddVedtak() {
  return useApiMutation<unknown, ProblemDetail, string>({
    mutationFn: (vedtakId) =>
      client.get({
        url: "/api/tiltaksadministrasjon/tilskudd/simuler-opphor/{vedtakId}",
        path: { vedtakId },
      }),
  });
}
