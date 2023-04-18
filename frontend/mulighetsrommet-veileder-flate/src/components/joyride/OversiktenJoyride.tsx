import Joyride, { ACTIONS, EVENTS, STATUS, Step } from 'react-joyride';
import { useEffect, useState } from 'react';
import { localeStrings } from './utils';
import { JoyrideKnapp } from './JoyrideKnapp';
import { logEvent } from '../../core/api/logger';
import { stepsOversikten } from './Steps';

interface Props {
  toggleHistorikkModal: (state: boolean) => void;
  isHistorikkModalOpen: boolean;
}

export function OversiktenJoyride({ toggleHistorikkModal, isHistorikkModalOpen }: Props) {
  const [state, setState] = useState({
    run: false,
    loading: false,
    stepIndex: 0,
  });

  useEffect(() => {
    //viser joyride ved første load
    return window.localStorage.getItem('joyride_oversikten') === null
      ? window.localStorage.setItem('joyride_oversikten', 'true')
      : window.localStorage.setItem('joyride_oversikten', 'false');
  }, []);

  useEffect(() => {
    if (isHistorikkModalOpen) {
      setState(prevState => ({ ...prevState, stepIndex: 7 }));
      toggleHistorikkModal(true);
    }
  }, [isHistorikkModalOpen]);

  const lastStep: Step[] = [
    {
      title: 'Tour',
      content: 'Hvis du vil se dette igjen kan du klikke her.',
      target: '#joyride_knapp',
      disableBeacon: true,
    },
  ];

  const handleJoyrideCallback = (data: { action: string; index: number; status: string; type: string }) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    // @ts-ignore
    if ([EVENTS.STEP_AFTER].includes(type)) {
      //må ha en timeout her for at joyride skal vente på at historikk-modalen vises
      if (EVENTS.TARGET_NOT_FOUND && index === 5) {
        setState(prevState => ({ ...prevState, run: false, loading: true }));
        setTimeout(() => {
          setState(prevState => ({ ...prevState, run: true, loading: false }));
        }, 200);
      }
      setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));

      //resetter joyride når den er ferdig eller man klikker skip
    } else {
      // @ts-ignore
      if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status)) {
        if (window.localStorage.getItem('joyride_oversikten-last-step') === null) {
          window.localStorage.setItem('joyride_oversikten-last-step', 'true');
        }
        setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
        window.localStorage.setItem('joyride_oversikten', 'false');
      }
    }

    if ([ACTIONS.NEXT] && index === 7) {
      toggleHistorikkModal(false);
    }
  };

  const handleJoyrideCallbackLastStep = (data: any) => {
    const { status, type } = data;
    if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status) || [EVENTS.TOOLTIP_CLOSE].includes(type)) {
      window.localStorage.setItem('joyride_oversikten-last-step', 'false');
    }
  };

  return (
    <>
      <JoyrideKnapp
        handleClick={() => {
          setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
          window.localStorage.setItem('joyride_oversikten', 'true');
          setState(prevState => ({ ...prevState, run: true }));
          logEvent('mulighetsrommet.joyride', { value: 'oversikten' });
        }}
      />
      <Joyride
        locale={localeStrings()}
        continuous
        run={window.localStorage.getItem('joyride_oversikten') === 'true'}
        steps={stepsOversikten}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
        stepIndex={state.stepIndex}
      />

      <Joyride
        locale={localeStrings()}
        run={window.localStorage.getItem('joyride_oversikten-last-step') === 'true'}
        steps={lastStep}
        callback={handleJoyrideCallbackLastStep}
      />
    </>
  );
}
