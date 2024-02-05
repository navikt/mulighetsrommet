import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import {
  useArbeidsmarkedstiltakFilterValue,
  valgteEnhetsnumre,
} from "@/hooks/useArbeidsmarkedstiltakFilter";

export function useVeilederTiltaksgjennomforinger() {
  const { queryIsValid, query } = useGetArbeidsmarkedstiltakFilterAsQuery();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforinger(query),
    queryFn: () => mulighetsrommetClient.veilederTiltak.getVeilederTiltaksgjennomforinger(query),
    enabled: queryIsValid,
  });
}

export function useNavTiltaksgjennomforinger({ preview }: { preview: boolean }) {
  const { queryIsValid, query } = useGetArbeidsmarkedstiltakFilterAsQuery();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforinger(query),
    queryFn() {
      return preview
        ? mulighetsrommetClient.veilederTiltak.getPreviewTiltaksgjennomforinger(query)
        : mulighetsrommetClient.veilederTiltak.getNavTiltaksgjennomforinger(query);
    },
    enabled: queryIsValid,
  });
}

function useGetArbeidsmarkedstiltakFilterAsQuery() {
  const filter = useArbeidsmarkedstiltakFilterValue();

  const tiltakstyper =
    filter.tiltakstyper.length !== 0 ? filter.tiltakstyper.map(({ id }) => id) : undefined;

  const enheter = valgteEnhetsnumre(filter);

  return {
    queryIsValid: enheter.length !== 0 && filter.innsatsgruppe !== undefined,
    query: {
      search: filter.search || undefined,
      apentForInnsok: filter.apentForInnsok,
      innsatsgruppe: filter.innsatsgruppe?.nokkel,
      enheter,
      tiltakstyper,
    },
  };
}
