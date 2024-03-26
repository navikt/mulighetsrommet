import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";

const FANT_IKKE_TILTAKSNUMMER_MESSAGE = "Fant ikke tiltaksnummer";

const RETRY_COUNT = 10;

const RETRY_DELAY = 3000;

export function usePollTiltaksnummer(id: string) {
  const tiltaksnummer = useQuery({
    queryKey: ["tiltakgjennomforing", id, "tiltaksnummer"],
    async queryFn() {
      const tiltaksnummer = await mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksnummer({
        id,
      });

      if (!tiltaksnummer) {
        throw Error(FANT_IKKE_TILTAKSNUMMER_MESSAGE);
      }

      return tiltaksnummer;
    },
    retry(retryCount, error) {
      return retryCount < RETRY_COUNT && error.message === FANT_IKKE_TILTAKSNUMMER_MESSAGE;
    },
    retryDelay: RETRY_DELAY,
    throwOnError: false,
  });

  return {
    isError: tiltaksnummer.isError,
    isLoading: tiltaksnummer.isLoading,
    data: tiltaksnummer.data,
  };
}
