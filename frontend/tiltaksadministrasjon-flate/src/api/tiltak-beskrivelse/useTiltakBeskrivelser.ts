import { TiltakBeskrivelseService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakBeskrivelseFilterType } from "@/pages/tiltak-beskrivelse/filter";
import { getPublisertStatus } from "@/utils/Utils";

export function useTiltakBeskrivelser(filter?: Partial<TiltakBeskrivelseFilterType>) {
  const request = {
    body: {
      navEnheter: filter?.navEnheter ?? [],
      tiltakstyper: filter?.tiltakstyper ?? [],
      publisert: getPublisertStatus(filter?.publisert) ?? null,
      sort: filter?.sortering?.sortString ?? null,
      visMineTiltakBeskrivelser: filter?.visMineTiltakBeskrivelser ?? false,
    },
    query: {
      page: filter?.page ?? 1,
      size: filter?.pageSize,
    },
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakBeskrivelser(request),
    queryFn: () => TiltakBeskrivelseService.getTiltakBeskrivelser(request),
  });
}
