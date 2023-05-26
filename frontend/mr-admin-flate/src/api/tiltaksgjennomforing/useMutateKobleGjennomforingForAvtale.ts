import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";

export function useMutateKobleGjennomforingForAvtale() {
  return useMutation({
    mutationFn: async (data: {
      gjennomforingId: string;
      avtaleId?: string;
    }) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.oppdaterAvtaleIdForGjennomforing(
        { id: data.gjennomforingId, requestBody: { avtaleId: data.avtaleId } }
      );
    },
  });
}
