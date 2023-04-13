import Joyride, { ACTIONS, EVENTS, STATUS, Step } from 'react-joyride';
import { useEffect, useState } from 'react';
import { localeStrings } from './utils';
import { JoyrideKnapp } from './JoyrideKnapp';
import { logEvent } from '../../core/api/logger';

interface Props {
  toggleHistorikkModal: (state: boolean) => void;
}

export function OversiktenJoyride({ toggleHistorikkModal }: Props) {
  const [state, setState] = useState({
    run: false,
    loading: false,
    stepIndex: 0,
  });

  useEffect(() => {
    //viser joyride ved første load
    return window.localStorage.getItem('joyrideOversikten') === null
      ? window.localStorage.setItem('joyrideOversikten', 'true')
      : window.localStorage.setItem('joyrideOversikten', 'false');
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
    {
      content: 'Her kan du lese mer om tiltaksgjennomføringene. Klikk på raden for å se!',
      placement: 'top',

      target: '#list_element',
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
      //for steps i historikk-modalen
      if (EVENTS.TARGET_NOT_FOUND && index === 5) {
        setState(prevState => ({ ...prevState, run: false, loading: true }));
        setTimeout(() => {
          setState(prevState => ({ ...prevState, run: true, loading: false }));
        }, 200);
      }
      setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));
    } else if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status)) {
      // if ([STATUS.SKIPPED].includes(status)) {
      if (window.localStorage.getItem('joyrideOversiktenLastStep') === null) {
        window.localStorage.setItem('joyrideOversiktenLastStep', 'true');
      }
      // }
      setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
      window.localStorage.setItem('joyrideOversikten', 'false');
    }
    // åpne historikk-modalen
    if ([ACTIONS.NEXT] && index === 6) {
      toggleHistorikkModal(true);
    }

    if ([ACTIONS.NEXT] && index === 7) {
      toggleHistorikkModal(false);
    }
  };

  const handleJoyrideCallbackLastStep = (data: any) => {
    const { status, type } = data;
    if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status) || [EVENTS.TOOLTIP_CLOSE].includes(type)) {
      window.localStorage.setItem('joyrideOversiktenLastStep', 'false');
    }
  };

  return (
    <>
      <JoyrideKnapp
        handleClick={() => {
          window.localStorage.setItem('joyrideOversikten', 'true');
          setState(prevState => ({ ...prevState, run: true }));
          logEvent('mulighetsrommet.joyride', { value: 'oversikten' });
        }}
      />
      <Joyride
        locale={localeStrings()}
        continuous
        run={window.localStorage.getItem('joyrideOversikten') === 'true'}
        steps={steps}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
        stepIndex={state.stepIndex}
      />

      <Joyride
        locale={localeStrings()}
        run={window.localStorage.getItem('joyrideOversiktenLastStep') === 'true'}
        steps={lastStep}
        callback={handleJoyrideCallbackLastStep}
      />
    </>
  );
}
