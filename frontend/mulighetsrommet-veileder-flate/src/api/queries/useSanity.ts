import { useQuery } from 'react-query';
import { QueryKeys } from '../../core/api/QueryKeys';
import { MulighetsrommetService } from 'mulighetsrommet-api-client';
import useDebounce from '../../hooks/useDebounce';

export function useSanity<T>(query: string) {
  const debouncedQuery = useDebounce(query, 300);
  return useQuery(
    [QueryKeys.SanityQuery, debouncedQuery],
    () => MulighetsrommetService.sanityQuery({ query: debouncedQuery }) as Promise<T>
  );
}
