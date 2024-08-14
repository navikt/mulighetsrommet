import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { VeilederTiltakService } from "@mr/api-client";

export function useInnsatsgrupper() {
  return useSuspenseQuery({
    queryKey: QueryKeys.sanity.innsatsgrupper,
    queryFn: () => VeilederTiltakService.getInnsatsgrupper(),
  });
}
