import { useApiQuery } from "@/hooks/useApiQuery";
import { useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingFilter } from "../atoms";
import { type GetGjennomforingerData, GjennomforingerService } from "@mr/api-client-v2";
import { getPublisertStatus } from "../../utils/Utils";

export function useAdminGjennomforinger(filter: Partial<GjennomforingFilter>) {
  const debouncedSok = useDebounce(filter.search?.trim(), 300);

  const queryFilter: GetGjennomforingerData = {
    query: {
      search: debouncedSok || undefined,
      navEnheter: filter.navEnheter?.map((e) => e.enhetsnummer) ?? [],
      tiltakstyper: filter.tiltakstyper,
      statuser: filter.statuser,
      sort: filter.sortering ? filter.sortering.sortString : undefined,
      page: filter.page ?? 1,
      size: filter.pageSize,
      avtaleId: filter.avtale ? filter.avtale : undefined,
      publisert: getPublisertStatus(filter.publisert),
      arrangorer: filter.arrangorer,
    },
  };

  return useApiQuery({
    queryKey: QueryKeys.gjennomforinger(filter.visMineGjennomforinger, queryFilter),
    queryFn: () =>
      filter.visMineGjennomforinger
        ? GjennomforingerService.getMineGjennomforinger(queryFilter)
        : GjennomforingerService.getGjennomforinger(queryFilter),
  });
}
