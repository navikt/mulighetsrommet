import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useMigrerteTiltakstyper() {
  return useQuery({
    queryKey: QueryKeys.migrerteTiltakstyper(),
    queryFn: () => mulighetsrommetClient.tiltakstyper.getMigrerteTiltakstyper(),
  });
}

export function useMigrerteTiltakstyperForAvtaler() {
  const { data = [], ...rest } = useMigrerteTiltakstyper();

  return { data: data?.concat("VASV", "ARBFORB"), ...rest };
}
