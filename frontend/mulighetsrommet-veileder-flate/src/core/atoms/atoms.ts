import { atomWithHash } from 'jotai/utils';

export interface Tiltaksgjennomforingsfilter {
  search?: string;
  innsatsgrupper: Tiltaksgjenomforingsfiltergruppe[];
  tiltakstyper: Tiltaksgjenomforingsfiltergruppe[];
}

export interface Tiltaksgjenomforingsfiltergruppe {
  id: string;
  tittel: string;
}

export const initialTiltaksgjennomforingsfilter = {
  search: '',
  innsatsgrupper: [],
  tiltakstyper: [],
};

export const tiltaksgjennomforingsfilter = atomWithHash<Tiltaksgjennomforingsfilter>(
  'filter',
  initialTiltaksgjennomforingsfilter
);

export const paginationAtom = atomWithHash('page', 1);
