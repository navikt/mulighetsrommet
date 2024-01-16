import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useQuery } from "@tanstack/react-query";
import { useArbeidsmarkedstiltakFilterValue } from "../../../hooks/useArbeidsmarkedstiltakFilter";

export const usePreviewTiltaksgjennomforinger = (geografiskEnhet?: string) => {
  const filter = useArbeidsmarkedstiltakFilterValue();

  const tiltakstypeIds =
    filter.tiltakstyper.length > 0 ? filter.tiltakstyper.map(({ id }) => id) : undefined;

  const requestBody = {
    geografiskEnhet: geografiskEnhet!!,
    innsatsgruppe: filter.innsatsgruppe?.nokkel,
    search: filter.search ? filter.search : undefined,
    tiltakstypeIds,
    apentForInnsok: filter.apentForInnsok,
  };

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforingerPreview(filter, geografiskEnhet),
    queryFn: () =>
      mulighetsrommetClient.veilederTiltak.getPreviewRelevanteTiltaksgjennomforinger({
        requestBody,
      }),
    enabled: !!geografiskEnhet,
  });
};
