import { MulighetsrommetService, SanityResponse } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { client } from '../sanityClient';
import { QueryKeys } from '../core/api/QueryKeys';

interface SanityHttpResponse<T> {
  ms: number;
  query: string;
  result: T[];
}

export function useSanity<T>(groqQuery: string) {
  // Dersom vi er i mock-modus, bruk sanity-client, hvis ikke bruk proxy fra backend
  const fetcher = import.meta.env.VITE_MULIGHETSROMMET_API_MOCK
    ? () => client.fetch(groqQuery)
    : () => MulighetsrommetService.getSanityQuery({ query: encodeURIComponent(groqQuery) });

  return useQuery<SanityHttpResponse<T>>(QueryKeys.SanityQuery, fetcher);
}
