import { QueryKeys } from "../QueryKeys";
import { ArrangorService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useArrangorBankKonto(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.arrangorBetalingsinfo(id),
    queryFn: () => ArrangorService.getBankKonto({ path: { id } }),
  });
}
