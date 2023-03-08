import { Bruker } from 'mulighetsrommet-api-client';
import { Tiltaksgjennomforingsfilter } from '../atoms/atoms';

export const QueryKeys = {
  SanityQuery: 'sanityQuery',
  Brukerdata: 'brukerdata',
  Veilederdata: 'veilederdata',
  Historikk: 'historikk',
  DeltMedBrukerStatus: 'deltMedBrukerStatus',
  sanity: {
    innsatsgrupper: ['innsatsgrupper'],
    tiltakstyper: ['tiltakstyper'],
    lokasjoner: ['lokasjoner'],
    tiltaksgjennomforinger: (bruker?: Bruker, tiltaksgjennomforingsfilter?: Tiltaksgjennomforingsfilter) => [
      'tiltaksgjennomforinger',
      { ...bruker },
      { ...tiltaksgjennomforingsfilter },
    ],
  },
};
