import { client } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakDokumentFilterType } from "@/pages/tiltak-dokument/filter";
import {
  GjennomforingKontaktpersonDto,
  GjennomforingDtoArrangorKontaktperson,
  Kontorstruktur,
  Faneinnhold,
} from "@tiltaksadministrasjon/api-client";

export interface TiltakDokumentTiltakstype {
  id: string;
  navn: string;
  tiltakskode: string;
}

export interface TiltakDokumentArrangor {
  id: string;
  navn: string;
  organisasjonsnummer: string;
}

export interface TiltakDokumentAdministrator {
  navIdent: string;
  navn: string;
}

export interface TiltakDokument {
  id: string;
  navn: string;
  tiltakstype: TiltakDokumentTiltakstype;
  stedForGjennomforing?: string | null;
  arrangor?: TiltakDokumentArrangor | null;
  faneinnhold?: Faneinnhold | null;
  beskrivelse?: string | null;
  publisert: boolean;
  administratorer: TiltakDokumentAdministrator[];
  kontorstruktur: Kontorstruktur[];
  kontaktpersoner: GjennomforingKontaktpersonDto[];
  arrangorKontaktpersoner: GjennomforingDtoArrangorKontaktperson[];
}

export function useTiltakDokumenter(filter?: Partial<TiltakDokumentFilterType>) {
  const request = {
    navEnheter: filter?.navEnheter ?? [],
    tiltakstyper: filter?.tiltakstyper ?? [],
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakDokumenter(request),
    queryFn: async (): Promise<{ data: TiltakDokument[] }> => {
      const result = await client.post<TiltakDokument[]>({
        url: "/api/tiltaksadministrasjon/tiltak-dokumenter/filter",
        body: request,
      });
      return { data: (result.data ?? []) as TiltakDokument[] };
    },
  });
}
