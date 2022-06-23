import { atom } from 'jotai';
import { Innsatsgruppe } from 'mulighetsrommet-api-client';
import { Tiltakstype } from '../../api/models';

export interface Tiltaksgjennomforingsfilter {
  search?: string;
  innsatsgrupper: Innsatsgruppe[];
  tiltakstyper?: Tiltakstype[];
}

export const tiltaksgjennomforingsfilter = atom<Tiltaksgjennomforingsfilter>({
  search: '',
  innsatsgrupper: [],
  tiltakstyper: [],
});
