import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "./useGetTiltaksgjennomforingIdFraUrl";

export default function usePreviewTiltaksgjennomforingById(brukersEnheter: string[]) {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforingPreview(id),
    queryFn: () =>
      mulighetsrommetClient.sanity.getTiltaksgjennomforingDetaljerPreview({
        requestBody: { id, brukersEnheter },
      }),
  });
}
