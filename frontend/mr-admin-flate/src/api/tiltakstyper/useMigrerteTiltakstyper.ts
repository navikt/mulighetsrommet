import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useMigrerteTiltakstyper() {
  return useQuery({
    queryKey: QueryKeys.migrerteTiltakstyper(),
    queryFn: () => mulighetsrommetClient.tiltakstyper.getMigrerteTiltakstyper(),
  });
}
