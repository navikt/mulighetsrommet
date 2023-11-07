import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "./useGetTiltaksgjennomforingIdFraUrl";

export default function usePreviewTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforingPreview(id),
    queryFn: () => mulighetsrommetClient.sanity.getTiltaksgjennomforingForPreview({ id }),
  });
}
