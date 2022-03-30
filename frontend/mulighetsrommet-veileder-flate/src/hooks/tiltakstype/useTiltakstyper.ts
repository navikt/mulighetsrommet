import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltakstype } from 'mulighetsrommet-api-client';
import { Tiltakstypefilter } from '../../core/atoms/atoms';
import { QueryKeys } from '../../core/api/QueryKeys';

export default function useTiltakstyper(filter: Tiltakstypefilter = {}) {
  return useQuery<Tiltakstype[]>([QueryKeys.Tiltakstyper, filter], () =>
    MulighetsrommetService.getTiltakstyper({
      ...filter,
      innsatsgrupper: filter.innsatsgrupper?.map(gruppe => gruppe.id),
    })
  );
}
