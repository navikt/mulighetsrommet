import { useHentBrukerdata } from './useHentBrukerdata';
import groq from 'groq';
import { useSanity } from './useSanity';

export default function useLokasjonerForBruker() {
  const brukerData = useHentBrukerdata();

  const sanityQueryString = groq`array::unique(*[_type == "tiltaksgjennomforing" && !(_id in path("drafts.**")) 
  ${byggEnhetOgFylkeFilter()}
  ]
  {
    lokasjon
  }.lokasjon)`;

  return useSanity<string[]>(sanityQueryString, {
    enabled: !!brukerData.data?.oppfolgingsenhet,
  });
}

function byggEnhetOgFylkeFilter(): string {
  return groq`&& ($enhetsId in enheter[]._ref || (enheter[0] == null && $fylkeId == fylke._ref))`;
}
