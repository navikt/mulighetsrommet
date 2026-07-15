import { client } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { IndividuellGjennomforingFilterType } from "@/pages/individuell-gjennomforing/filter";
import {
  GjennomforingKontaktpersonDto,
  GjennomforingDtoArrangorKontaktperson,
  Kontorstruktur,
  Faneinnhold,
} from "@tiltaksadministrasjon/api-client";

export interface IndividuellGjennomforingTiltakstype {
  id: string;
  navn: string;
  tiltakskode: string;
}

export interface IndividuellGjennomforingArrangor {
  id: string;
  navn: string;
  organisasjonsnummer: string;
}

export interface IndividuellGjennomforingAdministrator {
  navIdent: string;
  navn: string;
}

export interface IndividuellGjennomforing {
  id: string;
  navn: string;
  tiltakstype: IndividuellGjennomforingTiltakstype;
  stedForGjennomforing?: string | null;
  arrangor?: IndividuellGjennomforingArrangor | null;
  faneinnhold?: Faneinnhold | null;
  beskrivelse?: string | null;
  publisert: boolean;
  administratorer: IndividuellGjennomforingAdministrator[];
  kontorstruktur: Kontorstruktur[];
  kontaktpersoner: GjennomforingKontaktpersonDto[];
  arrangorKontaktpersoner: GjennomforingDtoArrangorKontaktperson[];
}

export function useIndividuelleGjennomforinger(
  filter?: Partial<IndividuellGjennomforingFilterType>,
) {
  const request = {
    navEnheter: filter?.navEnheter ?? [],
    tiltakstyper: filter?.tiltakstyper ?? [],
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.individuelleGjennomforinger(request),
    queryFn: async (): Promise<{ data: IndividuellGjennomforing[] }> => {
      const result = await client.post<IndividuellGjennomforing[]>({
        url: "/api/tiltaksadministrasjon/individuelle-gjennomforinger/filter",
        body: request,
      });
      return { data: (result.data ?? []) as IndividuellGjennomforing[] };
    },
  });
}
