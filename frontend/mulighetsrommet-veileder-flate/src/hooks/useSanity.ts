import { useQuery } from 'react-query';
import { QueryKeys } from '../core/api/QueryKeys';
import { client } from '../sanityClient';

export function useSanity<T>(groqQuery: string) {
  return useQuery([QueryKeys.SanityQuery, groqQuery], () => client.query<T>(groqQuery));
}
