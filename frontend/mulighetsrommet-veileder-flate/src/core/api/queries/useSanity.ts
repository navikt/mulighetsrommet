import { useQuery } from 'react-query';
import { QueryKeys } from '../query-keys';
import { MulighetsrommetService } from 'mulighetsrommet-api-client';
import useDebounce from '../../../hooks/useDebounce';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { useErrorHandler } from 'react-error-boundary';

/**
 *
 * @param query The Groq-query
 * @param enabled Wether to run the query or not
 * @param withoutUserdata If the query don't depend on user data, then you can set this value to true and save some time fetching your data
 * @returns
 */
export function useSanity<T>(query: string, enabled: boolean = true, withoutUserdata = false) {
  const debouncedQuery = useDebounce(query, 300);
  const fnr = useHentFnrFraUrl();
  const fnrValue = withoutUserdata ? undefined : fnr;
  const hook = useQuery(
    [QueryKeys.SanityQuery, debouncedQuery, fnr],
    () => MulighetsrommetService.sanityQuery({ query: debouncedQuery, fnr: fnrValue }) as Promise<T>,
    {
      enabled: !!enabled,
    }
  );
  useErrorHandler(hook.error);
  return hook;
}
