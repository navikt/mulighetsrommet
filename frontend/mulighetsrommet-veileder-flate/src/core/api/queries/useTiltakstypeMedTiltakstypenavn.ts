import groq from 'groq';
import { SanityTiltakstype } from 'mulighetsrommet-api-client';
import { useSanity } from './useSanity';

export function useTiltakstyperMedTiltakstypenavn(navn: string) {
  return useSanity<SanityTiltakstype>(
    groq`*[_type == "tiltakstype" && tiltakstypeNavn == "${navn}" && !(_id in path("drafts.**"))][0]{..., forskningsrapport[]->}`,
    {
      includeUserdata: false,
    }
  );
}
