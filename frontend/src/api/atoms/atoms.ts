import { atom } from 'jotai';
import { Innsatsgruppe } from '../models/Innsatsgruppe';

export interface Tiltakstypefilter {
  search?: string;
  innsatsgrupper?: Innsatsgruppe[];
}

export const tiltakstypefilter = atom<Tiltakstypefilter>({});
export const visSidemeny = atom<boolean>(true);
