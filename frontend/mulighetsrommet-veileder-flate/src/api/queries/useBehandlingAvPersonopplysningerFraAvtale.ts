import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { AvtalerService } from "mulighetsrommet-api-client";

export function useBehandlingAvPersonopplysningerFraAvtale(avtaleId?: string) {
  return useQuery({
    queryKey: QueryKeys.behandlingAvPersonopplysninger(avtaleId),
    queryFn: () => AvtalerService.getBehandlingAvPersonopplysninger({ id: avtaleId!! }),
    enabled: !!avtaleId,
  });
}
