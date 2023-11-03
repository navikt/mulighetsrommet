import { useAtom } from "jotai";
import { GetRelevanteTiltaksgjennomforingerPreviewRequest } from "mulighetsrommet-api-client";
import { useQuery } from "react-query";
import { tiltaksgjennomforingsfilter } from "../../atoms/atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export const usePreviewTiltaksgjennomforinger = (geografiskEnhet?: string) => {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);

  const requestBody: GetRelevanteTiltaksgjennomforingerPreviewRequest = {
    geografiskEnhet: geografiskEnhet!!,
    innsatsgruppe: filter.innsatsgruppe?.nokkel,
  };

  if (filter.search) {
    requestBody.search = filter.search;
  }

  if (filter.tiltakstyper.length > 0) {
    requestBody.tiltakstypeIds = filter.tiltakstyper.map(({ id }) => id);
  }

  return useQuery(
    QueryKeys.sanity.tiltaksgjennomforingerPreview(geografiskEnhet!!, filter),
    () => mulighetsrommetClient.sanity.getRelevanteTiltaksgjennomforingerPreview({ requestBody }),
    { enabled: !!geografiskEnhet },
  );
};
