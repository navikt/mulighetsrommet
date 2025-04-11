import { useQueryClient } from "@tanstack/react-query";
import {
  ArrangorKontaktperson,
  ArrangorKontaktpersonRequest,
  ArrangorService,
  ProblemDetail,
} from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useUpsertArrangorKontaktperson(arrangorId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<any, ProblemDetail, ArrangorKontaktpersonRequest>({
    mutationFn: (body: ArrangorKontaktpersonRequest) =>
      ArrangorService.upsertArrangorKontaktperson({
        path: { id: arrangorId },
        body,
      }),
    onSuccess(kontaktperson) {
      queryClient.setQueryData<ArrangorKontaktperson[]>(
        QueryKeys.arrangorKontaktpersoner(arrangorId),
        (previous) => {
          const kontaktpersoner = previous ?? [];
          if (kontaktpersoner.find((p) => p.id === kontaktperson.data.id)) {
            return kontaktpersoner.map((prevKontaktperson) =>
              prevKontaktperson.id === kontaktperson.data.id
                ? kontaktperson.data
                : prevKontaktperson,
            );
          } else return kontaktpersoner.concat(kontaktperson.data);
        },
      );
    },
  });
}
