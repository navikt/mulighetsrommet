import { useSanity } from './useSanity';
import { Tiltakstype } from '../models';
import groq from 'groq';

export function useTiltakstyper() {
  return useSanity<Tiltakstype[]>(groq`*[_type == "tiltakstype" && !(_id in path("drafts.**"))]`, {
    includeUserdata: false,
  });
}
