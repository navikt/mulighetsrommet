import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { Tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { QueryKeys } from '../../core/api/QueryKeys';

export default function useTiltaksgjennomforinger(filter: Tiltaksgjennomforingsfilter = {}) {
  return useQuery<Tiltaksgjennomforing[]>([QueryKeys.Tiltaksgjennomforinger, filter], () =>
    MulighetsrommetService.getTiltaksgjennomforinger({
      ...filter,
      innsatsgrupper: filter.innsatsgrupper?.map(gruppe => gruppe.id),
      tiltakstyper: filter.tiltakstyper?.map(type => type.id),
    })
  );
}
