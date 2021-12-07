import { Innsatsgruppe } from '../../core/domain/Innsatsgruppe';

export const innsatsgrupperMock: Innsatsgruppe[] = [
  {
    id: 1,
    tittel: 'Standardinnsats',
    beskrivelse: 'Gode muligheter',
  },
  {
    id: 2,
    tittel: 'Situasjonsbestemt innsats',
    beskrivelse: 'Trenger veiledning',
  },
  {
    id: 3,
    tittel: 'Spesielt tilpasset innsats',
    beskrivelse: 'Trenger veiledning, nedsatt arbeidsevne',
  },
  {
    id: 4,
    tittel: 'Varig tilpasset innsats',
    beskrivelse: 'Jobbe delvis eller liten mulighet til å jobbe',
  },
];
