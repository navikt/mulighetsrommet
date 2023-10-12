import { useMutation } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";

export function useMutateTilgjengeligForVeileder() {
  return useMutation({
    mutationFn: async (data: { id: string; tilgjengeligForVeileder: boolean }) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.setTilgjengeligForVeileder({
        id: data.id,
        requestBody: { tilgjengeligForVeileder: data.tilgjengeligForVeileder },
      });
    },
    useErrorBoundary: true,
  });
}
