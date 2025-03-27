import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { ArrangorService } from "@mr/api-client-v2";

export function useKontonummerForArrangor(orgnr: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.kontonummerArrangor(orgnr),
    queryFn: () => ArrangorService.getKontonummerForArrangor({ path: { orgnr } }),
  });
}
