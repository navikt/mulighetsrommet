import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "./useGetTiltaksgjennomforingIdFraUrl";
import {
  navEnheter,
  useArbeidsmarkedstiltakFilterValue,
} from "../../../hooks/useArbeidsmarkedstiltakFilter";

export default function usePreviewTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const filter = useArbeidsmarkedstiltakFilterValue();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforingPreview(id),
    queryFn: () =>
      mulighetsrommetClient.veilederTiltak.getPreviewTiltaksgjennomforing({
        requestBody: { id, enheter: navEnheter(filter) },
      }),
  });
}
