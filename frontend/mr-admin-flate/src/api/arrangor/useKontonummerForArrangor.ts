import { QueryKeys } from "../QueryKeys";
import { ArrangorService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useKontonummerForArrangor(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.kontonummerArrangor(id),
    queryFn: () => ArrangorService.getKontonummer({ path: { id } }),
  });
}
