import Joyride, { ACTIONS, STATUS } from 'react-joyride';

import React, { useState } from 'react';
import { localeStrings } from './utils';
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

  const handleJoyrideCallbackOpprettAvtale = (data: any) => {
    const { action, status } = data;
    //Hvis gjennomføringen ikke har opprett avtale, vises denne neste gang man går inn på en gjennomføring med opprett avtale
    if ([ACTIONS.CLOSE, ACTIONS.RESET].includes(action) || [STATUS.FINISHED, STATUS.SKIPPED].includes(status)) {
      setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: true });
      setState(prevState => ({ ...prevState, run: false, stepIndex: 0 }));
    }
  };

  return (
    <Joyride
      locale={localeStrings()}
      continuous
      run={joyride.joyrideDetaljerHarVistOpprettAvtale === false}
      steps={opprettAvtaleStep}
      hideCloseButton
      callback={handleJoyrideCallbackOpprettAvtale}
      showSkipButton
      disableOverlayClose
    />
  );
};
