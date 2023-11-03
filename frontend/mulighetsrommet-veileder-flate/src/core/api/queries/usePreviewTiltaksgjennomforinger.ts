import { useAtom } from "jotai";
import { useQuery } from "react-query";
import { tiltaksgjennomforingsfilter } from "../../atoms/atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export const usePreviewTiltaksgjennomforinger = (geografiskEnhet?: string) => {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);

  return useQuery(
    QueryKeys.sanity.tiltaksgjennomforingerPreview(filter, geografiskEnhet),
    () =>
      mulighetsrommetClient.sanity.getRelevanteTiltaksgjennomforingerPreview({
        requestBody: {
          geografiskEnhet: geografiskEnhet!!,
          innsatsgruppe: filter.innsatsgruppe?.nokkel,
          search: filter.search ? filter.search : undefined,
          tiltakstypeIds:
            filter.tiltakstyper.length > 0 ? filter.tiltakstyper.map(({ id }) => id) : undefined,
        },
      }),
    {
      enabled: !!geografiskEnhet,
    },
  );
};
