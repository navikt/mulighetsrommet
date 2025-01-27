import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  DelMedBrukerService,
  DelTiltakMedBrukerRequest,
  DelTiltakMedBrukerResponse,
} from "@mr/api-client-v2";
import { QueryKeys } from "../query-keys";

interface Props {
  onSuccess: (response: DelTiltakMedBrukerResponse) => void;
  onError: () => void;
}

export function useDelTiltakMedBruker({ onSuccess, onError }: Props) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: QueryKeys.DeltMedBrukerStatus,
    mutationFn: async (body: DelTiltakMedBrukerRequest) =>
      DelMedBrukerService.delTiltakMedBruker({ body }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: QueryKeys.DeltMedBrukerStatus });
      onSuccess(response.data!);
    },
    onError: () => onError(),
  });
}
