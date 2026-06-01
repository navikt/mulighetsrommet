import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { PersonopplysningService } from "@tiltaksadministrasjon/api-client";

export function usePersonopplysninger() {
  return useApiQuery({
    queryKey: QueryKeys.personopplysninger(),
    queryFn: () => PersonopplysningService.getPersonopplysninger(),
  });
}
