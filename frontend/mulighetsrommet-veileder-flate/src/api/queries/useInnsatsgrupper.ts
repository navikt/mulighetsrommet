import { Innsatsgruppe } from '../models';
import { useSanity } from './useSanity';

export function useInnsatsgrupper() {
  return useSanity<Innsatsgruppe[]>('*[_type == "innsatsgruppe"] | order(order asc)');
}
