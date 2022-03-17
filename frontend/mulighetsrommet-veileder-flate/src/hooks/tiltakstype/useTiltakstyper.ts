import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltakstype } from 'mulighetsrommet-api';
import { QueryKeys } from '../../core/api/QueryKeys';

export default function useTiltakstyper() {
  return useQuery<Tiltakstype[]>(QueryKeys.Tiltakstyper, MulighetsrommetService.getTiltakstyper);
}
