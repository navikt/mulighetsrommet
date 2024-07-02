import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstyperService } from "mulighetsrommet-api-client";

export function useMigrerteTiltakstyper() {
  return useQuery({
    queryKey: QueryKeys.migrerteTiltakstyper(),
    queryFn: () => TiltakstyperService.getMigrerteTiltakstyper(),
  });
}

export function useMigrerteTiltakstyperForAvtaler() {
  const { data = [], ...rest } = useMigrerteTiltakstyper();

  return { data: data?.concat("VASV", "ARBFORB"), ...rest };
}
