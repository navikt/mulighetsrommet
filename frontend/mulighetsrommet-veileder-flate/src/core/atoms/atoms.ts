import { atom } from 'jotai';
import { Innsatsgruppe } from '../../../../mulighetsrommet-api-client';

export interface Tiltakstypefilter {
  search?: string;
  innsatsgrupper?: Innsatsgruppe[];
}

export const tiltakstypefilter = atom<Tiltakstypefilter>({ search: '', innsatsgrupper: [] });
export const visSidemeny = atom<boolean>(true);
