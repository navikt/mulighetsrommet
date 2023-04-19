import Joyride, { ACTIONS, EVENTS, STATUS } from 'react-joyride';
import { useEffect, useState } from 'react';
import { localeStrings } from './utils';
import { JoyrideKnapp } from './JoyrideKnapp';
import { logEvent } from '../../core/api/logger';
import { stepsOversikten } from './Steps';
import { useAtom } from 'jotai';
import { joyrideAtom } from '../../core/atoms/atoms';

interface Props {
  toggleHistorikkModal: (state: boolean) => void;
  isHistorikkModalOpen: boolean;
}

export function OversiktenJoyride({ toggleHistorikkModal, isHistorikkModalOpen }: Props) {
  const [joyride, setJoyride] = useAtom(joyrideAtom);
  const [state, setState] = useState({
    run: false,
    loading: false,
    stepIndex: 0,
  });

  useEffect(() => {
    //viser joyride ved første load
    return joyride.joyrideOversikten === null
      ? setJoyride({ ...joyride, joyrideOversikten: true })
      : setJoyride({ ...joyride, joyrideOversikten: false });
  }, []);

  useEffect(() => {
    if (isHistorikkModalOpen) {
      setState(prevState => ({ ...prevState, stepIndex: 7 }));
      toggleHistorikkModal(true);
    }
  }, [isHistorikkModalOpen]);

  const handleJoyrideCallback = (data: any) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    if ([EVENTS.STEP_AFTER].includes(type)) {
      //må ha en timeout her for at joyride skal vente på at historikk-modalen vises
      if (EVENTS.TARGET_NOT_FOUND && index === 5) {
        setState(prevState => ({ ...prevState, run: false, loading: true }));
        setTimeout(() => {
          setState(prevState => ({ ...prevState, run: true, loading: false }));
        }, 200);
      }
      setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));
    }
    //resetter joyride når den er ferdig eller man klikker skip
    else if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status)) {
      setJoyride({ ...joyride, joyrideOversiktenLastStep: true, joyrideOversikten: false });
      setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
    }

    if ([ACTIONS.NEXT] && index === 7) {
      toggleHistorikkModal(false);
    }
  };

  return (
    <>
      <JoyrideKnapp
        handleClick={() => {
          setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
          setJoyride({ ...joyride, joyrideOversikten: true });
          setState(prevState => ({ ...prevState, run: true }));
          logEvent('mulighetsrommet.joyride', { value: 'oversikten' });
        }}
      />
      <Joyride
        locale={localeStrings()}
        continuous
        run={joyride.joyrideOversikten === true}
        steps={stepsOversikten}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
        stepIndex={state.stepIndex}
      />
    </>
  );
}
