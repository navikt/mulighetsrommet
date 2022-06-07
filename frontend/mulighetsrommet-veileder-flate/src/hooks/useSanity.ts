import { MulighetsrommetService } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { QueryKeys } from '../core/api/QueryKeys';

export function useSanity(groqQuery: string) {
  return useQuery(QueryKeys.SanityQuery, () =>
    MulighetsrommetService.getSanityQuery({ query: encodeURIComponent(groqQuery) })
  );
}
