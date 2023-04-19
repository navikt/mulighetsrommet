import Joyride, { ACTIONS, EVENTS, STATUS } from 'react-joyride';
import { useEffect, useState } from 'react';
import { localeStrings } from './utils';
import { JoyrideKnapp } from './JoyrideKnapp';
import { logEvent } from '../../core/api/logger';
import { stepsDetaljer } from './Steps';
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

  useEffect(() => {
    //viser joyride ved første load
    return joyride.joyrideDetaljer === null
      ? setJoyride({ ...joyride, joyrideDetaljer: true })
      : setJoyride({ ...joyride, joyrideDetaljer: false });
  }, []);

  const handleJoyrideCallback = (data: any) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    if (!opprettAvtale) {
      //hvis brukeren ikke er inne på et tiltak med opprett avtale, settes opprett avtale-steps til false i localStorage
      setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: false });

      //hopper over steget med opprett avtale for at den skal kjøre videre til neste steg
      if (index === 3 && [ACTIONS.NEXT]) {
        setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));
      }
    }

    if ([EVENTS.STEP_AFTER].includes(type)) {
      setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));
    }

    //resetter joyride når den er ferdig eller man klikker skip
    else if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status)) {
      setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
      setJoyride({ ...joyride, joyrideDetaljer: false });
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
        run={joyride.joyrideDetaljer === true}
        steps={stepsDetaljer}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
        stepIndex={state.stepIndex}
      />
    </>
  );
}
