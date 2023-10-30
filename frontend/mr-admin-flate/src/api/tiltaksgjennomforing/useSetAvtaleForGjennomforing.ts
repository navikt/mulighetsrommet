import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";

export function useSetAvtaleForGjennomforing() {
  return useMutation({
    mutationFn: async (data: { gjennomforingId: string; avtaleId?: string }) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.setAvtaleForGjennomforing({
        id: data.gjennomforingId,
        requestBody: {
          avtaleId: data.avtaleId,
        },
      });
    },
  });
}
