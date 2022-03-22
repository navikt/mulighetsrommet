import { atom } from 'jotai';
import { Innsatsgruppe } from '../../../../mulighetsrommet-api';

export interface Tiltakstypefilter {
  search?: string;
  innsatsgrupper?: Innsatsgruppe[];
}

export const tiltakstypefilter = atom<Tiltakstypefilter>({ search: '', innsatsgrupper: [] });
export const visSidemeny = atom<boolean>(true);
