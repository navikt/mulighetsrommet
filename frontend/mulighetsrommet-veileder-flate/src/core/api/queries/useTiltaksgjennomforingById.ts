import { SanityTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { erPreview } from '../../../utils/Utils';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useGetTiltaksgjennomforingIdFraUrl } from './useGetTiltaksgjennomforingIdFraUrl';
import { useFnr } from '../../../hooks/useFnr';

export default function useTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl().replace('drafts.', '');
  const fnr = useFnr();
  const response = erPreview
    ? useQuery(QueryKeys.sanity.tiltaksgjennomforingPreview(id), () =>
        mulighetsrommetClient.sanity.getSanityTiltaksgjennomforingForPreview({ id })
      )
    : useQuery(QueryKeys.sanity.tiltaksgjennomforing(id), () =>
        mulighetsrommetClient.sanity.getSanityTiltaksgjennomforing({ id, fnr })
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
