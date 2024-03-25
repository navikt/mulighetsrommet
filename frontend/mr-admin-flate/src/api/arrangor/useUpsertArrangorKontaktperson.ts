import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  ArrangorKontaktperson,
  ArrangorKontaktpersonRequest,
} from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useUpsertArrangorKontaktperson(arrangorId: string) {
  const queryClient = useQueryClient();

  return useMutation<ArrangorKontaktperson, ApiError, ArrangorKontaktpersonRequest>({
    mutationFn: (requestBody: ArrangorKontaktpersonRequest) =>
      mulighetsrommetClient.arrangor.upsertArrangorKontaktperson({
        id: arrangorId,
        requestBody,
      }),
    onSuccess(kontaktperson) {
      queryClient.setQueryData<ArrangorKontaktperson[]>(
        QueryKeys.arrangorKontaktpersoner(arrangorId),
        (previous) => {
          const kontaktpersoner = previous ?? [];
          if (kontaktpersoner.find((p) => p.id === kontaktperson.id)) {
            return kontaktpersoner.map((prevKontaktperson) =>
              prevKontaktperson.id === kontaktperson.id ? kontaktperson : prevKontaktperson,
            );
          } else return kontaktpersoner.concat(kontaktperson);
        },
      );
    },
  });
}
