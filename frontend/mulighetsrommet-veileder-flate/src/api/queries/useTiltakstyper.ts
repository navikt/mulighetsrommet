import { useSanity } from './useSanity';
import { Tiltakstype } from '../models';

export function useTiltakstyper() {
  return useSanity<Tiltakstype[]>(`*[_type == "tiltakstype"]{tiltakstypeNavn, _id}`);
}
