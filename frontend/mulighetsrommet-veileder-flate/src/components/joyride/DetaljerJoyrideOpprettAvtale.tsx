import Joyride, { ACTIONS, EVENTS, STATUS, Step } from 'react-joyride';
import { useState } from 'react';

interface Props {
  setDelMedBrukerModal: (state: boolean) => void;
  delMedBrukerModalOpen: boolean;
  opprettAvtale: boolean;
}
export function DetaljerJoyrideOpprettAvtale({ setDelMedBrukerModal, delMedBrukerModalOpen, opprettAvtale }: Props) {
  const [, setState] = useState({});
  const [opprettAvtaleStepToggle, setOpprettAvtaleStepToggle] = useState(opprettAvtale);

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
    //dette steget vil feile, men må være med for at den delemodalStep skal kjøre
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

  const opprettAvtaleStep: Step[] = [
    {
      content: 'For tiltak med {MER TEKST HER} kan du opprette avtale.',
      placement: 'auto',
      target: '#opprett-avtale-knapp',
      disableBeacon: true,
    },
  ];

  // TODO fiks opprett avtale, måten den er på nå er dårlig

  //hvis opprett-knappen synes, skal vi sette i localStorage at opprett avtale-steps allerede er vist
  if (opprettAvtale) {
    setOpprettAvtaleStepToggle(true);
  }

  const handleJoyrideCallback = (data: any) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    if ([EVENTS.STEP_AFTER].includes(type)) {
      //for dele-modalen
      if (EVENTS.TARGET_NOT_FOUND && index === 6) {
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
    if ([ACTIONS.NEXT] && index === 5) {
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

  const handleJoyrideCallbackOpprettAvtale = (data: any) => {
    const { action } = data;
    // sett opprett avtale til sett
    if ([ACTIONS.CLOSE].includes(action)) {
      setOpprettAvtaleStepToggle(false);
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
        disableOverlayClose
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
        run={opprettAvtaleStepToggle}
        steps={opprettAvtaleStep}
        hideCloseButton
        callback={handleJoyrideCallbackOpprettAvtale}
        showSkipButton
        disableOverlayClose
      />
    </>
  );
}
