import { useQuery } from 'react-query';
import { QueryKeys } from '../query-keys';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { useErrorHandler } from 'react-error-boundary';
import { mulighetsrommetClient } from '../clients';
import { useDebounce } from 'mulighetsrommet-frontend-common';

interface Options {
  enabled?: boolean;
  includeUserdata?: boolean;
}

/**
 *
 * @param query The Groq query to Sanity
 * @param options Use the options.enabled to decide wether to fetch or not. Use options.includeUserdata for queries dependent on user data
 * @returns
 */
export function useSanity<T>(
  query: string,
  options: Options = {
    enabled: true,
    includeUserdata: true,
  }
) {
  const { enabled = true, includeUserdata = true } = options;
  const debouncedQuery = useDebounce(query, 300);
  const fnr = useHentFnrFraUrl();
  const fnrValue = includeUserdata ? fnr : undefined;

  const hook = useQuery(
    [QueryKeys.SanityQuery, debouncedQuery, fnr, options],
    () => mulighetsrommetClient.sanity.sanityQuery({ query: debouncedQuery, fnr: fnrValue }) as Promise<T>,
    {
      enabled: !!enabled,
    }
  );
  useErrorHandler(hook.error);
  return hook;
}
