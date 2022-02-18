import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltakstype } from '../../api';
import { QueryKeys } from '../../api/QueryKeys';
import { Tiltakstypefilter } from '../../api/atoms/atoms';

export default function useTiltakstyper(filter: Tiltakstypefilter = {}) {
  return useQuery<Tiltakstype[]>([QueryKeys.Tiltakstyper, filter], () =>
    MulighetsrommetService.getTiltakstyper({
      ...filter,
      innsatsgrupper: filter.innsatsgrupper?.map(gruppe => gruppe.id),
    })
  );
}
