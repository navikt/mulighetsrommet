import { useSanity } from './useSanity';
import { Tiltakstype } from '../models';
import groq from 'groq';

export function useTiltakstyperMedTiltakstypenavn(navn: string) {
  return useSanity<Tiltakstype>(
    groq`*[_type == "tiltakstype" && tiltakstypeNavn == "${navn}" && !(_id in path("drafts.**"))][0]{..., forskningsrapport[]->}`
  );
}
