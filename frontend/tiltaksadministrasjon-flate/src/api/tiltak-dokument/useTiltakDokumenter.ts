import { TiltakDokumentService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakDokumentFilterType } from "@/pages/tiltak-dokument/filter";
import { getPublisertStatus } from "@/utils/Utils";

export function useTiltakDokumenter(filter?: Partial<TiltakDokumentFilterType>) {
  const request = {
    body: {
      navEnheter: filter?.navEnheter ?? [],
      tiltakstyper: filter?.tiltakstyper ?? [],
      publisert: getPublisertStatus(filter?.publisert) ?? null,
      sort: filter?.sortering?.sortString ?? null,
      visMineTiltakDokumenter: filter?.visMineTiltakDokumenter ?? false,
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
