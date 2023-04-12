import Joyride, { ACTIONS, EVENTS, STATUS, Step } from 'react-joyride';
import { useEffect, useState } from 'react';
import { localeStrings } from './utils';

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
    return window.localStorage.getItem('joyrideDetaljer') === null
      ? window.localStorage.setItem('joyrideDetaljer', 'true')
      : window.localStorage.setItem('joyrideDetaljer', 'false');
  }, []);

  useEffect(() => {
    if (window.localStorage.getItem('harVistJoyrideOpprettAvtale') === null && opprettAvtale) {
      return window.localStorage.setItem('harVistJoyrideOpprettAvtale', 'true');
    } else if (!opprettAvtale && window.localStorage.getItem('harVistJoyrideOpprettAvtale') === null) {
      return window.localStorage.setItem('harVistJoyrideOpprettAvtale', 'false');
    }
  }, []);

  const steps: Step[] = [
    {
      title: 'Tiltaksgjennomføring',
      content: 'Her kan du lese mer om tiltaket.',
      placement: 'auto',
      target: '#tiltaksgjennomforing_detaljer',
      disableBeacon: true,
    },
    {
      content: 'Du kan bytte fane her og lese mer informasjon om tiltaket.',
      placement: 'auto',
      target: '#fane_liste',
      disableBeacon: true,
    },
    {
      content: 'I dette området kan du lese mer nøkkelinfromasjon.',
      placement: 'auto',
      target: '#sidemeny',
      disableBeacon: true,
    },
    {
      content: 'For tiltak med {MER TEKST HER} kan du opprette avtale.',
      placement: 'auto',
      target: '#opprett-avtale-knapp',
      disableBeacon: true,
    },
    {
      content: 'Du kan også dele tiltaket med bruker via dialogen! Klikk neste for å se.',
      placement: 'auto',
      target: '#deleknapp',
      disableBeacon: true,
    },
    {
      content: 'Dette er teksten brukeren vil se i dialogen etter at tiltaket er delt.',
      placement: 'auto',
      styles: {
        options: {
          zIndex: 10000,
        },
      },
      target: '#deletekst',
      disableBeacon: true,
    },
    {
      content: 'Du kan også legge til mer tekst ved å klikke her.',
      placement: 'auto',
      styles: {
        options: {
          zIndex: 10000,
        },
      },
      target: '#personlig_melding_btn',
      disableBeacon: true,
    },
  ];

  const handleJoyrideCallback = (data: any) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    if (!opprettAvtale) {
      //hvis det ikke er tiltak med opprett avtale, settes opprett avtale-steps til ikke vist i localStorage
      window.localStorage.setItem('harVistJoyrideOpprettAvtale', 'false');
      //hopper over steget med opprett avtale for at den skal kjøre videre
      if (index === 3 && [ACTIONS.NEXT]) {
        setState(prevState => ({ ...prevState, stepIndex: nextStepIndex }));
      }
    }

    if ([EVENTS.STEP_AFTER].includes(type)) {
      //for dele-modalen
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
    } else if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status)) {
      //reset joyride
      setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
      window.localStorage.setItem('joyrideDetaljer', 'false');
      //lukker dele-modalen hvis det er på siste steget
      setDelMedBrukerModal(false);
    }

    // åpne dele-modalen
    if ([ACTIONS.NEXT] && index === 5) {
      setDelMedBrukerModal(true);
    }
  };

  return (
    <Joyride
      locale={localeStrings()}
      continuous
      run={window.localStorage.getItem('joyrideDetaljer') === 'true'}
      steps={steps}
      hideCloseButton
      callback={handleJoyrideCallback}
      showSkipButton
      stepIndex={state.stepIndex}
    />
  );
}
