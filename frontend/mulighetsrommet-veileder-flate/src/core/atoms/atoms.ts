import { atom } from 'jotai';
import { Innsatsgruppe, Tiltakstype } from '../../../../mulighetsrommet-api-client';

export interface Tiltaksgjennomforingsfilter {
  search?: string;
  innsatsgrupper?: Innsatsgruppe[];
  tiltakstyper?: Tiltakstype[];
}

export const tiltaksgjennomforingsfilter = atom<Tiltaksgjennomforingsfilter>({
  search: '',
  innsatsgrupper: [],
  tiltakstyper: [],
});
