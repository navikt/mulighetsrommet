import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from 'react-joyride';
import { useEffect, useState } from 'react';
import { joyrideKnappefarge, localeStrings } from './utils';
import { JoyrideKnapp } from './JoyrideKnapp';
import { logEvent } from '../../core/api/logger';
import { getStepIndex, isStep, stepsOversikten } from './Steps';
import { useAtom } from 'jotai';
import { joyrideAtom } from '../../core/atoms/atoms';

interface Props {
  setHistorikkModalOpen: (state: boolean) => void;
  isHistorikkModalOpen: boolean;
}

export function OversiktenJoyride({ setHistorikkModalOpen, isHistorikkModalOpen }: Props) {
  const [joyride, setJoyride] = useAtom(joyrideAtom);
  const [state, setState] = useState({
    run: false,
    loading: false,
    stepIndex: 0,
  });

  useEffect(() => {
    if (isHistorikkModalOpen) {
      setState(prevState => ({ ...prevState, stepIndex: getStepIndex(stepsOversikten, 'tiltakshistorikk-modal') }));
      setHistorikkModalOpen(true);
    }
  }, [isHistorikkModalOpen]);

  const handleJoyrideCallback = (data: CallBackProps) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    //kjører neste step når man klikker på neste
    if (EVENTS.STEP_AFTER === type) {
      setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));
    }

    //resetter joyride ved error
    if (STATUS.ERROR === status) {
      setJoyride({ ...joyride, joyrideOversikten: true });
      setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
    }

    //resetter joyride når den er ferdig eller man klikker skip
    else if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)) {
      if (joyride.joyrideOversiktenLastStep === null) {
        setJoyride({ ...joyride, joyrideOversiktenLastStep: true, joyrideOversikten: false });
      } else {
        setJoyride({ ...joyride, joyrideOversikten: false });
      }
      setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      setJoyride({ ...joyride, joyrideOversikten: false });
      setState(prevState => ({ ...prevState, run: true, stepIndex: 0 }));
      if (joyride.joyrideOversiktenLastStep === null) {
        setJoyride({ ...joyride, joyrideOversiktenLastStep: true, joyrideOversikten: false });
      }
    }

    //åpner historikk-modal ved klikk på neste
    if (isStep(data.step, 'tiltakshistorikk-knapp') && EVENTS.STEP_AFTER === type) {
      setHistorikkModalOpen(true);
    }

    //lukker historikk-modal
    if (isStep(data.step, 'detaljert-visning')) {
      setHistorikkModalOpen(false);
    }

    //lukker historikk-modal hvis man klikker på close
    if (
      isStep(data.step, 'tiltakshistorikk-modal') &&
      (STATUS.SKIPPED.includes(status) || STATUS.FINISHED.includes(status))
    ) {
      setHistorikkModalOpen(false);
    }

    //åpner historikk-modal om man klikker forrige på den etter
    if (isStep(data.step, 'detaljert-visning') && ACTIONS.PREV === action) {
      setHistorikkModalOpen(true);
    }

    // lukker historikk-modal om man klikker forrige i modalen
    if (isStep(data.step, 'tiltakshistorikk-modal') && ACTIONS.PREV === action) {
      setHistorikkModalOpen(false);
    }
  };

  return (
    <>
      <JoyrideKnapp
        handleClick={() => {
          setJoyride({ ...joyride, joyrideOversikten: true });
          setState(prevState => ({ ...prevState, run: true, stepIndex: 0 }));
          logEvent('mulighetsrommet.joyride', { value: 'oversikten' });
        }}
      />
      <Joyride
        locale={localeStrings()}
        continuous
        run={joyride.joyrideOversikten}
        steps={stepsOversikten}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
        stepIndex={state.stepIndex}
        disableScrolling
        styles={{
          options: {
            primaryColor: joyrideKnappefarge,
          },
        }}
        disableCloseOnEsc={false}
        disableOverlayClose={true}
      />
    </>
  );
}
