import { Step } from 'react-joyride';
import Lenke from '../lenke/Lenke';
import { BodyShort } from '@navikt/ds-react';
import { porten } from 'mulighetsrommet-frontend-common/constants';

export function getStepIndex(steps: MulighetsrommetStep[], stepId: string): number {
  return steps.findIndex(step => step.id === stepId);
}

export interface MulighetsrommetStep extends Step {
  id: string;
}

export function isStep(step: Step, stepId: string): boolean {
  return (step as MulighetsrommetStep).id === stepId;
}

export const stepsOversikten: MulighetsrommetStep[] = [
  {
    title: 'Velkommen til arbeidsmarkedstiltak!',
    content: 'Her får du hjelp til å finne arbeidsmarkedstiltak som passer brukerens ønsker og behov.',
    target: 'body',
    placement: 'center',
    disableBeacon: true,
    id: 'velkommen',
  },
  {
    title: 'Liste over tiltak',
    content: 'I listen finnes alle gjennomføringene som er tilknyttet brukerens NAV-enhet.',
    target: '[data-testid="oversikt_tiltaksgjennomforinger"]',
    disableBeacon: true,
    id: 'liste-over-tiltak',
  },
  {
    title: 'Prefiltrering',
    content: 'Gjennomføringene er allerede filtrert på brukerens innsatsbehov og geografiske tilknytning.',
    target: '[data-testid="filtertags"]',
    disableBeacon: true,
    id: 'prefiltrering',
  },
  {
    title: 'Filtrering',
    content: 'Du kan benytte filtrene for å tilpasse utvalget av tiltak til brukerens ønsker og behov.',
    target: '#tiltakstype_oversikt_filtermeny',
    disableBeacon: true,
    id: 'filtrering',
  },
  {
    title: 'Sortering',
    content: 'I tillegg kan du sortere listen med tiltaksgjennomføringer slik at de mest relevante kommer øverst.',
    target: '[data-testid="sortering-select"]',
    disableBeacon: true,
    id: 'sortering',
  },
  {
    title: 'Tiltakshistorikk',
    content:
      'Historikken gir deg oversikt over hvilke tiltak brukeren har deltatt på de siste fem årene. Klikk på ikonet for å se historikken.',
    target: '#historikk_knapp',
    placement: 'left',
    disableBeacon: true,
    disableOverlayClose: true,
    spotlightClicks: true,
    id: 'tiltakshistorikk-knapp',
  },
  {
    title: 'Tiltakshistorikk',
    content: 'I modalen får du oversikt over tiltakene brukeren har deltatt på, når, og hvor lenge.',
    target: '#historikk_modal',
    placement: 'bottom',
    styles: {
      options: {
        zIndex: 10000,
      },
    },
    disableBeacon: true,
    id: 'tiltakshistorikk-modal',
  },
  {
    title: 'Detaljert visning',
    content: 'Ved å klikke på gjennomføringen vil du kunne se flere detaljer om tiltaket.',
    target: '#list_element_0',
    placement: 'top',
    disableBeacon: true,
    id: 'detaljert-visning',
  },
];

export const stepsDetaljer: MulighetsrommetStep[] = [
  {
    title: 'Detaljside',
    content: 'Her finner du detaljert informasjon om tiltaket, som formål, målgruppe, innhold og varighet.',
    target: '#tiltaksgjennomforing_detaljer',
    disableBeacon: true,
    id: 'detaljside',
  },
  {
    title: 'Strukturert innhold',
    content: 'På detaljsiden er informasjonen strukturert i faner slik at du raskt finner det du leter etter.',
    target: '#fane_liste',
    disableBeacon: true,
    id: 'strukturert-innhold',
  },
  {
    title: 'Nøkkelinformasjon',
    content: 'I denne boksen finner du nøkkelinformasjon om tiltaket og lenker til Rundskriv og Forskrifter.',
    target: '#sidemeny',
    disableBeacon: true,
    id: 'nokkelinformasjon',
  },
  {
    title: 'Opprett avtale',
    content: 'For enkelte av tiltakene vil det være mulig å opprette en avtale med bruker.',
    target: '[data-testid="opprettavtaleknapp"]',
    disableBeacon: true,
    id: 'opprett-avtale',
  },
  {
    title: 'Dele med bruker',
    content: 'Del en predefinert tekst om tiltaket med bruker i dialogen.',
    target: '[data-testid="deleknapp"]',
    disableBeacon: true,
    id: 'dele-med-bruker',
  },
  {
    title: 'Tilbakemeldinger',
    content: (
      <BodyShort>
        Har du innspill eller forslag til forbedringer vil vi gjerne at du tar kontakt med oss i{' '}
        {
          <Lenke to={porten} target={'_blank'}>
            Porten
          </Lenke>
        }
        .
      </BodyShort>
    ),
    target: 'body',
    placement: 'center',
    disableBeacon: true,
    id: 'tilbakemeldinger',
  },
];

export const opprettAvtaleStep: MulighetsrommetStep[] = [
  {
    title: 'Opprett avtale',
    content: 'For enkelte av tiltakene vil det være mulig å opprette en avtale med bruker.',
    target: '[data-testid="opprettavtaleknapp"]',
    disableBeacon: true,
    id: 'opprett-avtale',
  },
];

export const stepsLastStep: MulighetsrommetStep[] = [
  {
    title: 'Se gjennomgang på nytt',
    content: 'Hvis du vil se denne gjennomgangen på nytt kan du klikke her.',
    target: '#joyride_knapp',
    disableBeacon: true,
    id: 'gjennomgang-knapp',
  },
];
