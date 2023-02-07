import { atomWithHash } from 'jotai-location';
import { InnsatsgruppeNokler } from '../api/models';

export interface Tiltaksgjennomforingsfilter {
  search?: string;
  innsatsgruppe?: Tiltaksgjennomforingsfiltergruppe<InnsatsgruppeNokler>;
  tiltakstyper: Tiltaksgjennomforingsfiltergruppe<string>[];
  tiltaksgruppe: Tiltaksgjennomforingsfiltergruppe<string>[];
  lokasjoner: Tiltaksgjennomforingsfiltergruppe<string>[];
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
  tiltaksgruppe: [],
  lokasjoner: [],
};

export const tiltaksgjennomforingsfilter = atomWithHash<Tiltaksgjennomforingsfilter>(
  'filter',
  initialTiltaksgjennomforingsfilter,
  {
    setHash: 'replaceState',
  }
);

export const paginationAtom = atomWithHash('page', 1);
export const faneAtom = atomWithHash('fane', 'tab1');
