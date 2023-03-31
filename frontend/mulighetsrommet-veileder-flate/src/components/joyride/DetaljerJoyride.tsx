import Joyride, { ACTIONS, EVENTS, STATUS, Step } from 'react-joyride';
import { useState } from 'react';

interface Props {
  setDelMedBrukerModal: (state: boolean) => void;
  delMedBrukerModalOpen: boolean;
}
export function DetaljerJoyride({ setDelMedBrukerModal, delMedBrukerModalOpen }: Props) {
  const [, setState] = useState({});

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
      content: 'Du kan også dele tiltaket med bruker via dialogen! Klikk neste for å se.',
      placement: 'auto',
      target: '#deleknapp',
      disableBeacon: true,
    },
    {
      content: 'Dette er teksten brukeren vil se i dialogen etter at tiltaket er delt.',
      placement: 'auto',
      target: '#deletekst',
      disableBeacon: true,
    },
  ];

  const delemodalStep: Step[] = [
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

    if ([EVENTS.STEP_AFTER].includes(type)) {
      //for dele-modalen
      if (EVENTS.TARGET_NOT_FOUND && index === 5) {
        setState({ run: false, loading: true });
        setTimeout(() => {
          setState({
            loading: false,
            run: true,
          });
        }, 200);
      }
      setState({ stepIndex: nextStepIndex });
    } else if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status)) {
      //reset joyride
      setState({ run: false, stepIndex: 0 });
    }

    // åpne dele-modalen
    if ([ACTIONS.NEXT] && index === 4) {
      setDelMedBrukerModal(true);
    }
  };

  const handleJoyrideCallbackDelemodal = (data: any) => {
    const { action } = data;
    // lukke historikk-modalen
    if ([ACTIONS.CLOSE].includes(action)) {
      setDelMedBrukerModal(false);
    }
  };

  return (
    <>
      <Joyride
        locale={{
          next: 'Neste',
          back: 'Forrige',
          close: 'Lukk',
          skip: 'Skip',
          last: 'Ferdig',
        }}
        continuous
        run={true}
        steps={steps}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
      />
      <Joyride
        locale={{
          next: 'Neste',
          back: 'Forrige',
          close: 'Lukk',
          skip: 'Skip',
          last: 'Ferdig',
        }}
        continuous
        run={delMedBrukerModalOpen}
        steps={delemodalStep}
        hideCloseButton
        callback={handleJoyrideCallbackDelemodal}
        showSkipButton
      />
    </>
  );
}
