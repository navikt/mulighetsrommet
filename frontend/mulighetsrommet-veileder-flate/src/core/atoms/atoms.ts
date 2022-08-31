import { atomWithHash } from 'jotai/utils';
import { InnsatsgruppeNokler } from '../api/models';

export interface Tiltaksgjennomforingsfilter {
  search?: string;
  innsatsgruppe?: InnsatsgruppeNokler;
  tiltakstyper: Tiltaksgjennomforingsfiltergruppe[];
}

export interface Tiltaksgjennomforingsfiltergruppe {
  id: string;
  tittel: string;
  nokkel?: string;
}

export const initialTiltaksgjennomforingsfilter = {
  search: '',
  innsatsgruppe: undefined,
  tiltakstyper: [],
};

export const tiltaksgjennomforingsfilter = atomWithHash<Tiltaksgjennomforingsfilter>(
  'filter',
  initialTiltaksgjennomforingsfilter
);

export const paginationAtom = atomWithHash('page', 1);
export const faneAtom = atomWithHash('fane', 'tab1');

export const feedbackTilfredshet = false;
