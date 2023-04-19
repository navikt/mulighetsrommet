import { Step } from 'react-joyride';
import Lenke from '../lenke/Lenke';
import { BodyShort } from '@navikt/ds-react';

export const stepsOversikten: Step[] = [
  {
    title: 'Velkommen til arbeidsmarkedstiltak!',
    content: 'Her får du hjelp til å finne arbeidsmarkedstiltak som passer brukerens ønsker og behov.',
    target: 'body',
    placement: 'center',
    disableBeacon: true,
  },
  {
    title: 'Liste over tiltak',
    content: 'I listen finnes alle gjennomføringene som er tilknyttet brukerens NAV-enhet.',
    target: '[data-testid="oversikt_tiltaksgjennomforinger"]',
    disableBeacon: true,
  },
  {
    title: 'Prefiltrering',
    content: 'Gjennomføringene er allerede filtrert på brukerens innsatsbehov og geografiske tilknytning.',
    target: '[data-testid="filtertags"]',
    disableBeacon: true,
  },
  {
    title: 'Filtrering',
    content: 'Du kan benytte filtrene for å tilpasse utvalget av tiltak til brukerens ønsker og behov.',
    target: '#tiltakstype_oversikt_filtermeny',
    disableBeacon: true,
  },
  {
    title: 'Sortering',
    content: 'I tillegg kan du sortere listen med tiltaksgjennomføringer slik at de mest relevante kommer øverst.',
    target: '[data-testid="sortering-select"]',
    disableBeacon: true,
  },
  {
    title: 'Tiltakshistorikk',
    content:
      'Historikken gir deg oversikt over hvilke tiltak brukeren har deltatt på de siste fem årene. Klikk på ikonet for å se historikken.',
    target: '#historikkBtn',
    placement: 'left',
    disableBeacon: true,
    disableOverlayClose: true,
    spotlightClicks: true,
    hideCloseButton: true,
    hideFooter: true,
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
  },
  {
    title: 'Detaljert visning',
    content: 'Ved å klikke på gjennomføringen vil du kunne se flere detaljer om tiltaket.',
    target: '#list_element_0',
    placement: 'top',
    disableBeacon: true,
  },
];

export const stepsDetaljer: Step[] = [
  {
    title: 'Detaljside',
    content: 'Her finner du detaljert informasjon om tiltaket, som formål, målgruppe, innhold og varighet.',
    target: '#tiltaksgjennomforing_detaljer',
    disableBeacon: true,
  },
  {
    title: 'Strukturert innhold',
    content: 'På detaljsiden er informasjonen strukturert i faner slik at du raskt finner det du leter etter.',
    target: '#fane_liste',
    disableBeacon: true,
  },
  {
    title: 'Nøkkelinformasjon',
    content: 'I denn boksen finner du nøkkelinformasjon om tiltaket og lenker til Rundskriv og Forskrifter.',
    target: '#sidemeny',
    disableBeacon: true,
  },
  {
    title: 'Opprett avtale',
    content: 'For enkelte av tiltakene vil det være mulig å opprette en avtale med bruker.',
    target: '[data-testid="opprettavtaleknapp"]',
    disableBeacon: true,
  },
  {
    title: 'Dele med bruker',
    content: 'Del en predefinert tekst om tiltaket med bruker i dialogen.',
    target: '[data-testid="deleknapp"]',
    disableBeacon: true,
  },
  {
    title: 'Tilbakemeldinger',
    content: (
      <BodyShort>
        Har du innspill eller forslag til forbedringer vil vi gjerne at du tar kontakt med oss i{' '}
        {
          <Lenke to={'https://jira.adeo.no/plugins/servlet/desk/portal/541/create/1401'} target={'_blank'}>
            Porten
          </Lenke>
        }
        .
      </BodyShort>
    ),

    target: 'body',
    placement: 'center',
    disableBeacon: true,
  },
];

export const opprettAvtaleStep: Step[] = [
  {
    title: 'Opprett avtale',
    content: 'For enkelte av tiltakene vil det være mulig å opprette en avtale med bruker.',
    target: '[data-testid="opprettavtaleknapp"]',
    disableBeacon: true,
  },
];

export const stepsLastStep: Step[] = [
  {
    title: 'Tour',
    content: 'Hvis du vil se dette igjen kan du klikke her.',
    target: '#joyride_knapp',
    disableBeacon: true,
  },
];
