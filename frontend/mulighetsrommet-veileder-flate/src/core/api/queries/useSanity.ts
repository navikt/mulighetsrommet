import { useQuery } from 'react-query';
import { QueryKeys } from '../query-keys';
import { MulighetsrommetService } from 'mulighetsrommet-api-client';
import useDebounce from '../../../hooks/useDebounce';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { useErrorHandler } from 'react-error-boundary';

export function useSanity<T>(query: string, enabled: boolean = true) {
  const debouncedQuery = useDebounce(query, 300);
  const fnr = useHentFnrFraUrl();
  const hook = useQuery(
    [QueryKeys.SanityQuery, debouncedQuery, fnr],
    () => MulighetsrommetService.sanityQuery({ query: debouncedQuery, fnr }) as Promise<T>,
    {
      enabled: !!enabled,
    }
  );
  useErrorHandler(hook.error);
  return hook;
}
