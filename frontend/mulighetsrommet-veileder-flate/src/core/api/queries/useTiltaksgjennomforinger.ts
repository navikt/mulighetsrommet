import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import {
  isFilterReady,
  useArbeidsmarkedstiltakFilterValue,
  valgteEnhetsnumre,
} from "@/hooks/useArbeidsmarkedstiltakFilter";

export function useVeilederTiltaksgjennomforinger() {
  const { isFilterReady, filter } = useGetArbeidsmarkedstiltakFilterAsQuery();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforinger(filter),
    queryFn: () => mulighetsrommetClient.veilederTiltak.getVeilederTiltaksgjennomforinger(filter),
    enabled: isFilterReady,
  });
}

export function useNavTiltaksgjennomforinger({ preview }: { preview: boolean }) {
  const { isFilterReady, filter } = useGetArbeidsmarkedstiltakFilterAsQuery();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforinger(filter),
    queryFn() {
      return preview
        ? mulighetsrommetClient.veilederTiltak.getPreviewTiltaksgjennomforinger(filter)
        : mulighetsrommetClient.veilederTiltak.getNavTiltaksgjennomforinger(filter);
    },
    enabled: isFilterReady,
  });
}

function useGetArbeidsmarkedstiltakFilterAsQuery() {
  const filter = useArbeidsmarkedstiltakFilterValue();

  const tiltakstyper =
    filter.tiltakstyper.length !== 0 ? filter.tiltakstyper.map(({ id }) => id) : undefined;

  const enheter = valgteEnhetsnumre(filter);

  return {
    isFilterReady: isFilterReady(filter),
    filter: {
      search: filter.search || undefined,
      apentForInnsok: filter.apentForInnsok,
      innsatsgruppe: filter.innsatsgruppe?.nokkel,
      enheter,
      tiltakstyper,
    },
  };
}
