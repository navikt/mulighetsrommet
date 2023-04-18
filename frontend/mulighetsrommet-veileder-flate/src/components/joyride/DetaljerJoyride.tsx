import Joyride, { ACTIONS, EVENTS, STATUS } from 'react-joyride';
import { useEffect, useState } from 'react';
import { localeStrings } from './utils';
import { JoyrideKnapp } from './JoyrideKnapp';
import { logEvent } from '../../core/api/logger';
import { stepsDetaljer } from './Steps';

interface Props {
  setDelMedBrukerModal: (state: boolean) => void;
  opprettAvtale: boolean;
}
export function DetaljerJoyride({ setDelMedBrukerModal, opprettAvtale }: Props) {
  const [state, setState] = useState({
    run: false,
    loading: false,
    stepIndex: 0,
  });

  useEffect(() => {
    //viser joyride ved første load
    return window.localStorage.getItem('joyride_detaljer') === null
      ? window.localStorage.setItem('joyride_detaljer', 'true')
      : window.localStorage.setItem('joyride_detaljer', 'false');
  }, []);

  useEffect(() => {
    if (window.localStorage.getItem('joyride_har-vist-opprett-avtale') === null && opprettAvtale) {
      return window.localStorage.setItem('joyride_har-vist-opprett-avtale', 'true');
    } else if (!opprettAvtale && window.localStorage.getItem('joyride_har-vist-opprett-avtale') === null) {
      return window.localStorage.setItem('joyride_har-vist-opprett-avtale', 'false');
    }
  }, []);

  const handleJoyrideCallback = (data: { action: string; index: number; status: string; type: string }) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    if (!opprettAvtale) {
      //hvis brukeren ikke er inne på et tiltak med opprett avtale, settes opprett avtale-steps til false i localStorage
      window.localStorage.setItem('joyride_har-vist-opprett-avtale', 'false');

      //hopper over steget med opprett avtale for at den skal kjøre videre til neste steg
      if (index === 3 && [ACTIONS.NEXT]) {
        setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));
      }
    }

    // @ts-ignore
    if ([EVENTS.STEP_AFTER].includes(type)) {
      //må ha en timeout her for at joyride skal vente på at dele-modalen vises
      if (EVENTS.TARGET_NOT_FOUND && index === 6) {
        setState(prevState => ({ ...prevState, run: false, loading: true }));
        setTimeout(() => {
          setState(prevState => ({
            ...prevState,
            run: true,
            loading: false,
          }));
        }, 200);
      }
      setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));

      //resetter joyride når den er ferdig eller man klikker skip
    } else {
      // @ts-ignore
      if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status)) {
        setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
        window.localStorage.setItem('joyride_detaljer', 'false');
        //lukker dele-modalen hvis det er på siste steget
        setDelMedBrukerModal(false);
      }
    }

    // åpne dele-modalen
    if ([ACTIONS.NEXT] && index === 5) {
      setDelMedBrukerModal(true);
    }
  };

  return (
    <>
      <JoyrideKnapp
        handleClick={() => {
          window.localStorage.setItem('joyride_detaljer', 'true');
          setState(prevState => ({ ...prevState, run: true }));
          logEvent('mulighetsrommet.joyride', { value: 'detaljer' });
        }}
      />
      <Joyride
        locale={localeStrings()}
        continuous
        run={window.localStorage.getItem('joyride_detaljer') === 'true'}
        steps={stepsDetaljer}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
        stepIndex={state.stepIndex}
      />
    </>
  );
}
