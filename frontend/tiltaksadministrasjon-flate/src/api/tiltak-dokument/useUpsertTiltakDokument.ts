import { client } from "@tiltaksadministrasjon/api-client";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import { TiltakDokument } from "./useTiltakDokumenter";

export interface TiltakDokumentKontaktpersonRequest {
  navIdent: string;
  beskrivelse?: string | null;
}

export interface TiltakDokumentRequest {
  id: string;
  navn: string;
  tiltakstypeId: string;
  stedForGjennomforing?: string | null;
  arrangorId?: string | null;
  arrangorKontaktpersoner: string[];
  faneinnhold?: unknown | null;
  beskrivelse?: string | null;
  administratorer: string[];
  navRegioner: string[];
  navKontorer: string[];
  navAndreEnheter: string[];
  kontaktpersoner: TiltakDokumentKontaktpersonRequest[];
}

export function useUpsertTiltakDokument() {
  const queryClient = useQueryClient();

  return useApiMutation<TiltakDokument, unknown, TiltakDokumentRequest>({
    mutationFn: async (body) => {
      const result = await client.put<TiltakDokument>({
        url: "/api/tiltaksadministrasjon/tiltak-dokumenter",
        body,
      });
      return result.data as TiltakDokument;
    },
    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltakDokumenter(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltakDokument(request.id),
        }),
      ]);
    },
  });
}
