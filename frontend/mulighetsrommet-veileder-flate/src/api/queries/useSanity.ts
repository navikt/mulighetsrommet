import { useQuery } from 'react-query';
import { QueryKeys } from '../../core/api/QueryKeys';
import { MulighetsrommetService } from 'mulighetsrommet-api-client';
import useDebounce from '../../hooks/useDebounce';
import { useHentFnrFraUrl } from '../../hooks/useHentFnrFraUrl';

export function useSanity<T>(query: string, enabled: boolean = true) {
  const debouncedQuery = useDebounce(query, 300);
  const fnr = useHentFnrFraUrl();
  return useQuery(
    [QueryKeys.SanityQuery, debouncedQuery, fnr],
    () => MulighetsrommetService.sanityQuery({ query: debouncedQuery, fnr }) as Promise<T>,
    {
      enabled: !!enabled,
    }
  );
}
