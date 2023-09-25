import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";

export function useDeleteTiltaksgjennomforingsnotat() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => {
      return mulighetsrommetClient.tiltaksgjennomforingNotater.slettTiltaksgjennomforingNotat({
        id,
      });
    },
    onSuccess: () => {
      client.invalidateQueries({
        queryKey: ["tiltaksgjennomforingsnotater"],
      });
    },
  });
}
