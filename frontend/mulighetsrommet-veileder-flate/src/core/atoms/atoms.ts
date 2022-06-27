import { atom } from 'jotai';
import { Innsatsgruppe } from 'mulighetsrommet-api-client';
import { Tiltakstype } from '../../api/models';

export interface Tiltaksgjennomforingsfilter {
  search?: string;
  innsatsgrupper: Tiltaksgjenomforingsfiltergruppe[];
  tiltakstyper: Tiltaksgjenomforingsfiltergruppe[];
}

export interface Tiltaksgjenomforingsfiltergruppe {
  id: string,
  tittel: string
}

export const tiltaksgjennomforingsfilter = atom<Tiltaksgjennomforingsfilter>({
  search: '',
  innsatsgrupper: [],
  tiltakstyper: [],
});
