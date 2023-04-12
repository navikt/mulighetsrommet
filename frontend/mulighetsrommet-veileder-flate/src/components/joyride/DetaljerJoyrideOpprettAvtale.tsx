import Joyride, { ACTIONS, Step } from 'react-joyride';
import { localeStrings } from './utils';

export function DetaljerJoyrideOpprettAvtale() {
  const opprettAvtaleStep: Step[] = [
    {
      content: 'For tiltak med {MER TEKST HER} kan du opprette avtale.',
      placement: 'auto',
      target: '#opprett-avtale-knapp',
      disableBeacon: true,
    },
  ];
  //Hvis gjennomføringen ikke har opprett avtale, vis denne neste gang den har det
  const handleJoyrideCallbackOpprettAvtale = (data: any) => {
    const { action, step } = data;

    // sett opprett avtale til sett
    if (step.target === '#opprett-avtale-knapp' && [ACTIONS.CLOSE, ACTIONS.RESET].includes(action)) {
      console.log('HASFHAHSF');
      window.localStorage.setItem('harVistJoyrideOpprettAvtale', 'true');
    }
  };

  return (
    <>
      <Joyride
        locale={localeStrings()}
        continuous
        run={
          window.localStorage.getItem('harVistJoyrideOpprettAvtale') === 'false' &&
          window.localStorage.getItem('joyrideDetaljer') === 'false'
        }
        steps={opprettAvtaleStep}
        hideCloseButton
        callback={handleJoyrideCallbackOpprettAvtale}
        showSkipButton
        disableOverlayClose
      />
    </>
  );
}
