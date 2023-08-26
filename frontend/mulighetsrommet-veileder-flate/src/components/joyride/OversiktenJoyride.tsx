import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from 'react-joyride';
import { useEffect } from 'react';
import { locale, styling } from './config';
import { JoyrideKnapp } from './JoyrideKnapp';
import { logEvent } from '../../core/api/logger';
import { getStepIndex, isStep, oversiktenSteps, useSteps } from './Steps';
import { useAtom } from 'jotai';
import { joyrideAtom } from '../../core/atoms/atoms';

interface Props {
  setHistorikkModalOpen: (state: boolean) => void;
  isHistorikkModalOpen: boolean;
  isTableFetched: boolean;
}

export function OversiktenJoyride({ setHistorikkModalOpen, isHistorikkModalOpen, isTableFetched }: Props) {
  const [joyride, setJoyride] = useAtom(joyrideAtom);

  const ready = joyride.joyrideOversikten && isTableFetched;

  const { steps, stepIndex, setStepIndex } = useSteps(ready, oversiktenSteps);

  useEffect(() => {
    if (isHistorikkModalOpen) {
      setStepIndex(getStepIndex(steps, 'tiltakshistorikk-modal'));
      setHistorikkModalOpen(true);
    }
  }, [isHistorikkModalOpen]);

  const handleJoyrideCallback = (data: CallBackProps) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    //kjører neste step når man klikker på neste
    if (EVENTS.STEP_AFTER === type) {
      setStepIndex(nextStepIndex);
    }

    //resetter joyride ved error
    if (STATUS.ERROR === status) {
      setJoyride({ ...joyride, joyrideOversikten: true });
      setStepIndex(0);
    }

    //resetter joyride når den er ferdig eller man klikker skip
    else if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)) {
      logEvent('mulighetsrommet.joyride', { value: 'oversikten', status });
      if (joyride.joyrideOversiktenLastStep === null) {
        setJoyride({ ...joyride, joyrideOversiktenLastStep: true, joyrideOversikten: false });
      } else {
        setJoyride({ ...joyride, joyrideOversikten: false });
      }
      setStepIndex(0);
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      setJoyride({ ...joyride, joyrideOversikten: false });
      setStepIndex(0);
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
          setStepIndex(0);
          logEvent('mulighetsrommet.joyride', { value: 'oversikten' });
        }}
      />
      <Joyride
        locale={locale}
        continuous
        run={ready}
        steps={steps}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
        stepIndex={stepIndex}
        disableScrolling
        styles={styling}
        disableCloseOnEsc={false}
        disableOverlayClose={true}
      />
    </>
  );
}
