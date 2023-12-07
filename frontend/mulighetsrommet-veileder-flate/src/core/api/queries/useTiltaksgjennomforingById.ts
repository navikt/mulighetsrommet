import { useQuery } from "@tanstack/react-query";
import { useAppContext } from "../../../hooks/useAppContext";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "./useGetTiltaksgjennomforingIdFraUrl";

export default function useTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const { fnr } = useAppContext();

  const requestBody = { norskIdent: fnr, id };

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforing(id),
    queryFn: () =>
      mulighetsrommetClient.veilederTiltak.getTiltaksgjennomforingForBruker({ requestBody }),
  });
}
