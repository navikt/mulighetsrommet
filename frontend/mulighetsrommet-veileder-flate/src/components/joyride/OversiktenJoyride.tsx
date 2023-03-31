import Joyride, { ACTIONS, EVENTS, STATUS, Step } from 'react-joyride';
import { useEffect, useState } from 'react';
import { Button } from '@navikt/ds-react';
import { QuestionmarkIcon } from '@navikt/aksel-icons';
import { useJoyrideStepIndex } from '../../hooks/useJoyrideStepIndex';

interface Props {
  toggleHistorikkModal: (state: boolean) => void;
  isHistorikkModalOpen: boolean;
}

export function OversiktenJoyride({ toggleHistorikkModal, isHistorikkModalOpen }: Props) {
  const [, setState] = useState({});
  const [lastStepState, setLastStepState] = useState(false);
  const { setStepIndexState } = useJoyrideStepIndex();
  const localeStrings = () => ({
    next: 'Neste',
    back: 'Forrige',
    close: 'Lukk',
    skip: 'Skip',
    last: 'Ferdig',
  });

  useEffect(() => {
    //viser joyride ved første load
    return window.localStorage.getItem('joyrideOversikten') === null
      ? window.localStorage.setItem('joyrideOversikten', `true`)
      : window.localStorage.setItem('joyrideOversikten', `false`);
  }, []);

  const steps: Step[] = [
    {
      title: 'Velkommen til arbeidsmarkedstiltak!',
      content: 'Nå skal vi vise dere masse nye kule ting!',
      placement: 'center',
      target: 'body',
    },
    {
      target: '#gjennomforinger-liste',
      content: 'Her finner du alle tiltaksgjennomføringer for din NAV-enhet.',
      placement: 'auto',
      disableBeacon: true,
    },
    {
      target: '#filtertags',
      content: 'Listen er allerede filtrert på NAV-enhet og brukerens innsatsgruppe...',
      placement: 'auto',
      disableBeacon: true,
    },
    {
      target: '#tiltakstype_oversikt_filtermeny',
      content: '...men du kan også filtrere på andre ting som tiltakstyper og lokasjon!',
      placement: 'auto',
      disableBeacon: true,
    },
    {
      target: '#sortering-select',
      content: 'I tillegg kan du sortere listen her.',
      placement: 'auto',
      disableBeacon: true,
    },
    {
      target: '#historikkBtn',
      content: 'Her kan du se hvilke tiltak brukeren har vært på tidligere.',
      placement: 'left',
      disableBeacon: true,
    },
    //dette steget vil feile, men må være med for at den historikkStep skal kjøre
    {
      content: 'Her kan du se historikken til brukeren i kontekst.',
      placement: 'top',
      target: '#historikk_modal',
      disableBeacon: true,
    },
    {
      content: 'Her kan du lese mer om tiltaksgjennomføringene. Klikk på raden for å se!',
      placement: 'top',

      target: '#list_element',
      disableBeacon: true,
    },
  ];

  const historikkStep: Step[] = [
    {
      content: 'Her kan du se historikken til brukeren i kontekst.',
      placement: 'top',
      styles: {
        options: {
          zIndex: 10000,
        },
      },
      target: '#historikk_modal',
      disableBeacon: true,
    },
  ];

  const lastStep: Step[] = [
    {
      target: '#joyride_knapp',
      content: 'Hvis du vil se dette igjen kan du klikke her!',
      placement: 'auto',
      disableBeacon: true,
    },
  ];

  const handleJoyrideCallback = (data: any) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    if ([EVENTS.STEP_AFTER].includes(type)) {
      //for historikk-modalen
      if (EVENTS.TARGET_NOT_FOUND && index === 5) {
        setState({ run: false, loading: true });
        setTimeout(() => {
          setState({
            loading: false,
            run: true,
          });
        }, 200);
      }
      setStepIndexState(nextStepIndex);
    } else if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status)) {
      if ([STATUS.SKIPPED].includes(status)) {
        setLastStepState(true);
      }
      //reset joyride
      setStepIndexState(0);
      setState({ run: false });
      window.localStorage.setItem('joyrideOversikten', 'false');
      console.log('local', window.localStorage.getItem('joyrideOversikten'));
    }

    // åpne historikk-modalen
    if ([ACTIONS.NEXT] && index === 6) {
      toggleHistorikkModal(true);
    }
    console.log(type, index, action, status);
  };

  const handleJoyrideCallbackLastStep = (data: any) => {
    const { status, type } = data;
    if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status) || [EVENTS.TOOLTIP_CLOSE].includes(type)) {
      setLastStepState(false);
    }
  };

  const handleJoyrideCallbackHistorikkmodal = (data: any) => {
    const { action } = data;
    // lukke historikk-modalen
    if ([ACTIONS.CLOSE].includes(action)) {
      toggleHistorikkModal(false);
    }
  };

  return (
    <>
      <Button
        variant="secondary"
        onClick={() => {
          window.localStorage.setItem('joyrideOversikten', 'true');
        }}
        id="joyride_knapp"
      >
        <QuestionmarkIcon title="Virtuell omvisning" fontSize="1.5rem" />
      </Button>
      <Joyride
        locale={localeStrings()}
        continuous
        run={window.localStorage.getItem('joyrideOversikten') === 'true'}
        steps={steps}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
      />
      <Joyride locale={localeStrings()} run={lastStepState} steps={lastStep} callback={handleJoyrideCallbackLastStep} />
      <Joyride
        locale={localeStrings()}
        run={isHistorikkModalOpen}
        steps={historikkStep}
        callback={handleJoyrideCallbackHistorikkmodal}
      />
    </>
  );
}
