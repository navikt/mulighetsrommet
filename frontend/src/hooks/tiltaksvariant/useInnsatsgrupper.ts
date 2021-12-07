import { useQuery } from 'react-query';
import { InnsatsgrupperService } from '../../core/api/InnsatsgrupperService';
import { QueryKeys } from '../../core/api/QueryKeys';
import { Innsatsgruppe } from '../../core/domain/Innsatsgruppe';

export function useInnsatsgrupper() {
  return useQuery<Innsatsgruppe[]>(QueryKeys.Innsatsgrupper, InnsatsgrupperService.getInnsatsgrupper);
}
