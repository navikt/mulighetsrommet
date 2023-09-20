import { useQuery } from 'react-query';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useGetTiltaksgjennomforingIdFraUrl } from './useGetTiltaksgjennomforingIdFraUrl';

export default function usePreviewTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  return useQuery(QueryKeys.sanity.tiltaksgjennomforingPreview(id), () =>
    mulighetsrommetClient.sanity.getTiltaksgjennomforingForPreview({ id })
  );
}
