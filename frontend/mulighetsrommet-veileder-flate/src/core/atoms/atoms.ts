import { atomWithHash } from 'jotai/utils';
import { InnsatsgruppeNokler } from '../api/models';

export interface Tiltaksgjennomforingsfilter {
  search?: string;
  innsatsgruppe?: Tiltaksgjennomforingsfiltergruppe<InnsatsgruppeNokler>;
  tiltakstyper: Tiltaksgjennomforingsfiltergruppe<string>[];
  typeTiltak: Tiltaksgjennomforingsfiltergruppe<string>[];
}

export interface Tiltaksgjennomforingsfiltergruppe<T> {
  id: string;
  tittel: string;
  nokkel?: T;
}

export const initialTiltaksgjennomforingsfilter = {
  search: '',
  innsatsgruppe: undefined,
  tiltakstyper: [],
  typeTiltak: [],
};

export const tiltaksgjennomforingsfilter = atomWithHash<Tiltaksgjennomforingsfilter>(
  'filter',
  initialTiltaksgjennomforingsfilter
);

export const paginationAtom = atomWithHash('page', 1);
export const faneAtom = atomWithHash('fane', 'tab1');

export const feedbackTilfredshet = false;
