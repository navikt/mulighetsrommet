import { BodyShort } from "@navikt/ds-react";
import { APPLICATION_WEB_COMPONENT_NAME } from "@/constants";
import { useEffect, useState } from "react";
import { Step } from "react-joyride";
import { PortenLink } from "@/components/PortenLink";

export interface MulighetsrommetStep extends Step {
  id: string;
}

export function isStep(step: Step | null, stepId: string): boolean {
  return (step as MulighetsrommetStep | null)?.id === stepId;
}

export function useSteps(ready: boolean | null, initialSteps: MulighetsrommetStep[]) {
  const [stepIndex, setStepIndex] = useState(0);

  const [steps, setSteps] = useState<MulighetsrommetStep[]>([]);
  useEffect(() => {
    setSteps(ready ? prepareSteps(initialSteps) : []);
  }, [ready]);

  return {
    steps,
    stepIndex,
    setStepIndex,
  };
}

/**
 * If the application is contained within a web component we need to access each step's target contained by the
 * ShadowRoot instead of querying the DOM directly.
 *
 * It's important that the ShadowRoot is "open" so that we can access the content of the shadow DOM.
 */
function prepareSteps(steps: MulighetsrommetStep[]): MulighetsrommetStep[] {
  const shadowRoot = document.querySelector(APPLICATION_WEB_COMPONENT_NAME)?.shadowRoot;

  const resolveTarget = shadowRoot
    ? (target: string) => {
        return shadowRoot.querySelector<HTMLElement>(target) ?? target;
      }
    : (target: string) => target;

  return steps.map((step) => {
    return typeof step.target === "string" ? { ...step, target: resolveTarget(step.target) } : step;
  });
}

export const oversiktenSteps: MulighetsrommetStep[] = [
  {
    title: "Velkommen til arbeidsmarkedstiltak!",
    content:
      "Her får du hjelp til å finne arbeidsmarkedstiltak som passer brukerens ønsker og behov.",
    target: "body",
    placement: "center",
    disableBeacon: true,
    id: "velkommen",
  },
  {
    title: "Liste over tiltak",
    content: "I listen finnes alle gjennomføringene som er tilknyttet brukerens NAV-enhet.",
    target: '[data-testid="oversikt_tiltaksgjennomforinger"]',
    disableBeacon: true,
    id: "liste-over-tiltak",
  },
  {
    title: "Prefiltrering",
    content:
      "Gjennomføringene er allerede filtrert på brukerens innsatsbehov og geografiske tilknytning.",
    target: '[data-testid="filtertags"]',
    disableBeacon: true,
    id: "prefiltrering",
  },
  {
    title: "Filtrering",
    content:
      "Du kan benytte filtrene for å tilpasse utvalget av tiltak til brukerens ønsker og behov." +
      " Åpne/lukke filteroversikten ved å klikke her.",
    target: '[data-testid="filtertabs"]',
    disableBeacon: true,
    id: "filtrering",
  },
  {
    title: "Sortering",
    content:
      "I tillegg kan du sortere listen med tiltaksgjennomføringer slik at de mest relevante kommer øverst.",
    target: '[data-testid="sortering-select"]',
    disableBeacon: true,
    id: "sortering",
  },
  {
    title: "Tiltakshistorikk",
    content:
      "Historikken gir deg oversikt over hvilke tiltak brukeren har deltatt på de siste fem årene. Klikk på ikonet for å se historikken.",
    target: "#historikk_knapp",
    placement: "left",
    disableBeacon: true,
    disableOverlayClose: true,
    spotlightClicks: true,
    id: "tiltakshistorikk-knapp",
  },
  {
    title: "Detaljert visning",
    content: "Ved å klikke på gjennomføringen vil du kunne se flere detaljer om tiltaket.",
    target: "#list_element_0",
    placement: "top",
    disableBeacon: true,
    id: "detaljert-visning",
  },
  {
    title: "Se på nytt",
    content: "Hvis du vil se denne gjennomgangen på nytt kan du trykke på Veiviseren her",
    target: "#joyride_knapp",
    placement: "top",
    disableBeacon: true,
    id: "joyride_knapp",
  },
];

export const detaljerSteps: MulighetsrommetStep[] = [
  {
    title: "Detaljside",
    content:
      "Her finner du detaljert informasjon om tiltaket, som formål, målgruppe, innhold og varighet.",
    target: "#tiltaksgjennomforing_detaljer",
    disableBeacon: true,
    id: "detaljside",
  },
  {
    title: "Strukturert innhold",
    content:
      "På detaljsiden er informasjonen strukturert i faner slik at du raskt finner det du leter etter.",
    target: "#fane_liste",
    disableBeacon: true,
    id: "strukturert-innhold",
  },
  {
    title: "Nøkkelinformasjon",
    content:
      "I denne boksen finner du nøkkelinformasjon om tiltaket og lenker til Rundskriv og Forskrifter.",
    target: "#sidemeny",
    disableBeacon: true,
    id: "nokkelinformasjon",
  },
  {
    title: "Opprett avtale",
    content: "For enkelte av tiltakene vil det være mulig å opprette en avtale med bruker.",
    target: '[data-testid="opprettavtaleknapp"]',
    disableBeacon: true,
    id: "opprett-avtale",
  },
  {
    title: "Dele med bruker",
    content:
      "Del en predefinert tekst om tiltaket med bruker i dialogen. Du kan redigere teksten før sending ved behov.",
    target: '[data-testid="deleknapp"]',
    disableBeacon: true,
    id: "dele-med-bruker",
  },
  {
    title: "Tilbakemeldinger",
    content: (
      <BodyShort>
        Har du innspill eller forslag til forbedringer vil vi gjerne at du tar kontakt med oss i{" "}
        <PortenLink />.
      </BodyShort>
    ),
    target: "body",
    placement: "center",
    disableBeacon: true,
    id: "tilbakemeldinger",
  },
];

export const opprettAvtaleSteps: MulighetsrommetStep[] = [
  {
    title: "Opprett avtale",
    content: "For enkelte av tiltakene vil det være mulig å opprette en avtale med bruker.",
    target: '[data-testid="opprettavtaleknapp"]',
    disableBeacon: true,
    id: "opprett-avtale",
  },
];
