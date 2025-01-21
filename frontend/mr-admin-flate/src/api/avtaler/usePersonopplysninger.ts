import { useApiQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "@/api/QueryKeys";
import { PersonopplysningerService } from "@mr/api-client-v2";

export function usePersonopplysninger() {
  return useApiQuery({
    queryKey: QueryKeys.personopplysninger(),
    queryFn: () => PersonopplysningerService.getPersonopplysninger(),
  });
}
