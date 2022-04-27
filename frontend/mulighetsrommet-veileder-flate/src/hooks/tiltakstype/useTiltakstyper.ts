import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltakstype } from 'mulighetsrommet-api-client';
import { Tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { QueryKeys } from '../../core/api/QueryKeys';

export default function useTiltakstyper(filter: Tiltaksgjennomforingsfilter = {}) {
  return useQuery<Tiltakstype[]>([QueryKeys.Tiltakstyper, filter], () =>
    MulighetsrommetService.getTiltakstyper({
      ...filter,
      innsatsgrupper: undefined,
    })
  );
}
