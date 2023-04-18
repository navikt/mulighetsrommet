import Joyride, { ACTIONS } from 'react-joyride';
import { localeStrings } from './utils';
import { opprettAvtaleStep } from './Steps';

interface Props {
  opprettAvtale: boolean;
}

export function DetaljerOpprettAvtaleJoyride({ opprettAvtale }: Props) {
  //Hvis gjennomføringen ikke har opprett avtale, vises denne neste gang man går inn på en gjennomføring med opprett avtale
  const handleJoyrideCallbackOpprettAvtale = (data: any) => {
    const { action, step } = data;
    if (step.target === '#opprett-avtale-knapp' && [ACTIONS.CLOSE, ACTIONS.RESET].includes(action)) {
      window.localStorage.setItem('joyride_har-vist-opprett-avtale', 'true');
    }
  };

  return (
    <>
      {opprettAvtale ? (
        <Joyride
          locale={localeStrings()}
          continuous
          run={
            window.localStorage.getItem('joyride_har-vist-opprett-avtale') === 'false' &&
            window.localStorage.getItem('joyride_detaljer') === 'false'
          }
          steps={opprettAvtaleStep}
          hideCloseButton
          callback={handleJoyrideCallbackOpprettAvtale}
          showSkipButton
          disableOverlayClose
        />
      ) : null}
    </>
  );
}
