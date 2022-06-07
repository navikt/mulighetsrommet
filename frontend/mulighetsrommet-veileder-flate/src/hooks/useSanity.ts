import { MulighetsrommetService } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { client } from '../sanityClient';
import { QueryKeys } from '../core/api/QueryKeys';

export function useSanity(groqQuery: string) {
  // Dersom vi er i mock-modus, bruk sanity-client, hvis ikke bruk proxy fra backend
  const fetcher = import.meta.env.VITE_MULIGHETSROMMET_API_MOCK
    ? () => client.fetch(groqQuery)
    : () => MulighetsrommetService.getSanityQuery({ query: encodeURIComponent(groqQuery) });

  return useQuery(QueryKeys.SanityQuery, fetcher);
}
