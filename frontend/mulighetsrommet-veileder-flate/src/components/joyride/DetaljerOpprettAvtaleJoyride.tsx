import Joyride, { ACTIONS, CallBackProps, STATUS } from 'react-joyride';
import React, { useState } from 'react';
import { joyrideStyling, localeStrings } from './Utils';
import { opprettAvtaleStep } from './Steps';
import { useAtom } from 'jotai';
import { joyrideAtom } from '../../core/atoms/atoms';

interface Props {
  opprettAvtale: boolean;
}

export const DetaljerOpprettAvtaleJoyride = ({ opprettAvtale }: Props) => {
  const [joyride, setJoyride] = useAtom(joyrideAtom);
  const [, setState] = useState({
    run: false,
    loading: false,
    stepIndex: 0,
  });

  if (opprettAvtale && joyride.joyrideDetaljerHarVistOpprettAvtale === null) {
    setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: true });
    setState(prevState => ({ ...prevState, run: false }));
  } else if (!opprettAvtale && joyride.joyrideDetaljerHarVistOpprettAvtale === null) {
    setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: false });
    setState(prevState => ({ ...prevState, run: true }));
  }

  const handleJoyrideCallbackOpprettAvtale = (data: CallBackProps) => {
    const { action, status } = data;
    //Hvis gjennomføringen ikke har opprett avtale, vises denne neste gang man går inn på en gjennomføring med opprett avtale
    if (
      ([ACTIONS.CLOSE, ACTIONS.RESET] as string[]).includes(action) ||
      ([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)
    ) {
      setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: true });
      setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: true });
      setState(prevState => ({ ...prevState, run: true, stepIndex: 0 }));
    }
  };

  return (
    <Joyride
      locale={localeStrings()}
      continuous
      run={!joyride.joyrideDetaljerHarVistOpprettAvtale}
      steps={opprettAvtaleStep}
      hideCloseButton
      callback={handleJoyrideCallbackOpprettAvtale}
      showSkipButton
      disableScrolling
      styles={joyrideStyling()}
      disableCloseOnEsc={false}
      disableOverlayClose={true}
    />
  );
};
