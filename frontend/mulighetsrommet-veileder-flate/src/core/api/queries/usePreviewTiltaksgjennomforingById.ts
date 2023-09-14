import { useQuery } from 'react-query';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useGetTiltaksgjennomforingIdFraUrl } from './useGetTiltaksgjennomforingIdFraUrl';

export default function usePreviewTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const response = useQuery(QueryKeys.sanity.tiltaksgjennomforingPreview(id), () =>
    mulighetsrommetClient.sanity.getSanityTiltaksgjennomforingForPreview({ id })
  );

  return response;
}
