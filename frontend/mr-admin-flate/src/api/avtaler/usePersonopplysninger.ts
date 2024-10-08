import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { PersonopplysningerService } from "@mr/api-client";

export function usePersonopplysninger() {
  return useQuery({
    queryKey: QueryKeys.personopplysninger(),
    queryFn: () => PersonopplysningerService.getPersonopplysninger(),
  });
}
