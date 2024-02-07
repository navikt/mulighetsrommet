import { useQuery } from "@tanstack/react-query";
import { GetTiltaksgjennomforingerRequest } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import {
  useArbeidsmarkedstiltakFilterValue,
  valgteEnhetsnumre,
} from "@/hooks/useArbeidsmarkedstiltakFilter";

export function useVeilederTiltaksgjennomforinger() {
  return useGetTiltaksgjennomforinger(
    mulighetsrommetClient.veilederTiltak.getVeilederTiltaksgjennomforinger,
  );
}

export function useNavTiltaksgjennomforinger({ preview }: { preview: boolean }) {
  const queryFn = preview
    ? mulighetsrommetClient.veilederTiltak.getPreviewTiltaksgjennomforinger
    : mulighetsrommetClient.veilederTiltak.getNavTiltaksgjennomforinger;

  return useGetTiltaksgjennomforinger(queryFn);
}

function useGetTiltaksgjennomforinger(
  queryFn: typeof mulighetsrommetClient.veilederTiltak.getVeilederTiltaksgjennomforinger,
) {
  const filter = useArbeidsmarkedstiltakFilterValue();
  const requestBody: GetTiltaksgjennomforingerRequest = {
    enheter: valgteEnhetsnumre(filter),
    innsatsgruppe: filter.innsatsgruppe?.nokkel,
    apentForInnsok: filter.apentForInnsok,
  };

  if (filter.search) {
    requestBody.search = filter.search;
  }

  if (filter.tiltakstyper.length > 0) {
    requestBody.tiltakstypeIds = filter.tiltakstyper.map(({ id }) => id);
  }

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforinger(filter),
    queryFn: queryFn.bind(mulighetsrommetClient.veilederTiltak, { requestBody }),
  });
}
