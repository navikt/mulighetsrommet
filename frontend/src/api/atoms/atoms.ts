import { atom } from 'jotai';

export interface Tiltakstypefilter {
  search?: string;
  innsatsgrupper?: number[];
}

export const tiltakstypefilter = atom<Tiltakstypefilter>({});
export const visSidemeny = atom<boolean>(true);
