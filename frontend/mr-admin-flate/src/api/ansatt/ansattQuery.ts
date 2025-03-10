import { AnsattService } from "@mr/api-client-v2";
import { queryOptions } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";

export const ansattQuery = queryOptions({
  queryKey: QueryKeys.ansatt(),
  queryFn: async () => (await AnsattService.hentInfoOmAnsatt()).data,
});
