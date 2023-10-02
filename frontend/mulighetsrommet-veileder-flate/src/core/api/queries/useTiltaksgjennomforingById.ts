import { useQuery } from "react-query";
import { useFnr } from "../../../hooks/useFnr";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "./useGetTiltaksgjennomforingIdFraUrl";

export default function useTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const fnr = useFnr();

  const requestBody = { sanityId: id, norskIdent: fnr };

  return useQuery(QueryKeys.sanity.tiltaksgjennomforing(id), () =>
    mulighetsrommetClient.sanity.getTiltaksgjennomforingForBruker({ requestBody }),
  );
}
