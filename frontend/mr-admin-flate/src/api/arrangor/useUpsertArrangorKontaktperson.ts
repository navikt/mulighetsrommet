import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ArrangorKontaktperson,
  ArrangorKontaktpersonRequest,
  ArrangorService,
} from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

export function useUpsertArrangorKontaktperson(arrangorId: string) {
  const queryClient = useQueryClient();

  return useMutation({
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
