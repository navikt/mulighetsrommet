import { TiltakDokumentService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakDokumentFilterType } from "@/pages/tiltak-dokument/filter";

export function useTiltakDokumenter(filter?: Partial<TiltakDokumentFilterType>) {
  const request = {
    body: {
      navEnheter: filter?.navEnheter ?? [],
      tiltakstyper: filter?.tiltakstyper ?? [],
      sort: filter?.sortering?.sortString ?? null,
    },
    query: {
      page: filter?.page ?? 1,
      size: filter?.pageSize,
    },
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakDokumenter(request),
    queryFn: () => TiltakDokumentService.getTiltakDokumenter(request),
  });
}
