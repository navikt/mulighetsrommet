import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from 'react-joyride';
import { useState } from 'react';
import { joyrideStyling, localeStrings } from './Utils';
import { JoyrideKnapp } from './JoyrideKnapp';
import { logEvent } from '../../core/api/logger';
import { isStep, stepsDetaljer } from './Steps';
import { useAtom } from 'jotai';
import { joyrideAtom } from '../../core/atoms/atoms';

interface Props {
  opprettAvtale: boolean;
}
export function DetaljerJoyride({ opprettAvtale }: Props) {
  const [joyride, setJoyride] = useAtom(joyrideAtom);
  const [state, setState] = useState({
    run: false,
    loading: false,
    stepIndex: 0,
  });

  const handleJoyrideCallback = (data: CallBackProps) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    //kjører neste step når man klikker på neste
    if (EVENTS.STEP_AFTER === type) {
      setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));
    }

    if (!opprettAvtale) {
      //hvis brukeren ikke er inne på et tiltak med opprett avtale, settes opprett avtale-steps til false i localStorage
      setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: false });

      //hopper over steget med opprett avtale for at den skal kjøre videre til neste steg
      if (isStep(data.step, 'opprett-avtale') && !opprettAvtale) {
        setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));
      }
    }

    //resetter joyride ved error
    if (STATUS.ERROR === status) {
      setJoyride({ ...joyride, joyrideDetaljer: true });
      setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
    }

    //resetter joyride når den er ferdig eller man klikker skip
    else if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)) {
      logEvent('mulighetsrommet.joyride', { value: 'oversikten', status });
      setJoyride({ ...joyride, joyrideDetaljer: false });
      setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      setJoyride({ ...joyride, joyrideDetaljer: false });
      setState(prevState => ({ ...prevState, run: true, stepIndex: 0 }));
    }
  };

  return (
    <>
      <JoyrideKnapp
        handleClick={() => {
          setJoyride({ ...joyride, joyrideDetaljer: true });
          setState(prevState => ({ ...prevState, run: true }));
          logEvent('mulighetsrommet.joyride', { value: 'detaljer' });
        }}
      />
      <Joyride
        locale={localeStrings()}
        continuous
        run={joyride.joyrideDetaljer}
        steps={stepsDetaljer}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
        stepIndex={state.stepIndex}
        disableScrolling
        styles={joyrideStyling()}
        disableCloseOnEsc={false}
        disableOverlayClose={true}
      />
    </>
  );
}
