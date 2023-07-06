import { SanityTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { erPreview } from '../../../utils/Utils';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useGetTiltaksgjennomforingIdFraUrl } from './useGetTiltaksgjennomforingIdFraUrl';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';

export default function useTiltaksgjennomforingById() {
  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFraUrl().replace('drafts.', '');
  const fnrForBruker = useHentFnrFraUrl();
  const response = useQuery(QueryKeys.sanity.tiltaksgjennomforing(tiltaksgjennomforingId), () =>
    mulighetsrommetClient.sanity.getSanityTiltaksgjennomforing({ id: tiltaksgjennomforingId, fnr: fnrForBruker })
  );

  if (!response.data) {
    return response;
  }
  return { ...response, data: filterDataToSingleItem(response.data, erPreview) };
}

function filterDataToSingleItem(data: SanityTiltaksgjennomforing | SanityTiltaksgjennomforing[], preview: boolean) {
  if (!Array.isArray(data)) {
    return data;
  }

  if (data.length === 1) {
    return data[0];
  }

  if (preview) {
    return data.find(item => item._id.startsWith(`drafts.`)) || data[0];
  }

  return data[0];
}
