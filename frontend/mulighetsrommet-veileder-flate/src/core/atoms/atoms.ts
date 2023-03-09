import { atomWithHash } from 'jotai-location';
import { Innsatsgruppe } from 'mulighetsrommet-api-client';

export interface Tiltaksgjennomforingsfilter {
  search?: string;
  innsatsgruppe?: Tiltaksgjennomforingsfiltergruppe<Innsatsgruppe>;
  tiltakstyper: Tiltaksgjennomforingsfiltergruppe<string>[];
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
