import { atomWithHash } from 'jotai/utils';

export interface Tiltaksgjennomforingsfilter {
  search?: string;
  innsatsgrupper: Tiltaksgjennomforingsfiltergruppe[];
  tiltakstyper: Tiltaksgjennomforingsfiltergruppe[];
}

export interface Tiltaksgjennomforingsfiltergruppe {
  id: string;
  tittel: string;
  nokkel?: string;
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
export const faneAtom = atomWithHash('fane', 'tab1');
