import { client } from "@tiltaksadministrasjon/api-client";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import { IndividuellGjennomforing } from "./useIndividuelleGjennomforinger";

export interface IndividuellGjennomforingKontaktpersonRequest {
  navIdent: string;
  beskrivelse?: string | null;
}

export interface IndividuellGjennomforingRequest {
  id: string;
  navn: string;
  tiltakstypeId?: string | null;
  stedForGjennomforing?: string | null;
  arrangorId?: string | null;
  arrangorKontaktpersoner: string[];
  faneinnhold?: unknown | null;
  beskrivelse?: string | null;
  administratorer: string[];
  navRegioner: string[];
  navKontorer: string[];
  navAndreEnheter: string[];
  kontaktpersoner: IndividuellGjennomforingKontaktpersonRequest[];
}

export function useUpsertIndividuellGjennomforing() {
  const queryClient = useQueryClient();

  return useApiMutation<IndividuellGjennomforing, unknown, IndividuellGjennomforingRequest>({
    mutationFn: async (body) => {
      const result = await client.put<IndividuellGjennomforing>({
        url: "/api/tiltaksadministrasjon/individuelle-gjennomforinger",
        body,
      });
      return result.data as IndividuellGjennomforing;
    },
    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.individuelleGjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.individuellGjennomforing(request.id),
        }),
      ]);
    },
  });
}
