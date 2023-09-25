import Joyride, { ACTIONS, CallBackProps, STATUS } from "react-joyride";
import React from "react";
import { locale, styling } from "./config";
import { useAtom } from "jotai";
import { joyrideAtom } from "../../core/atoms/atoms";
import { opprettAvtaleSteps, useSteps } from "./Steps";

interface Props {
  opprettAvtale: boolean;
}

export const DetaljerOpprettAvtaleJoyride = ({ opprettAvtale }: Props) => {
  const [joyride, setJoyride] = useAtom(joyrideAtom);

  const { steps } = useSteps(joyride.joyrideDetaljerHarVistOpprettAvtale, opprettAvtaleSteps);

  if (opprettAvtale && joyride.joyrideDetaljerHarVistOpprettAvtale === null) {
    setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: true });
  } else if (!opprettAvtale && joyride.joyrideDetaljerHarVistOpprettAvtale === null) {
    setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: false });
  }

  const handleJoyrideCallbackOpprettAvtale = (data: CallBackProps) => {
    const { action, status } = data;
    //Hvis gjennomføringen ikke har opprett avtale, vises denne neste gang man går inn på en gjennomføring med opprett avtale
    if (
      ([ACTIONS.CLOSE, ACTIONS.RESET] as string[]).includes(action) ||
      ([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)
    ) {
      setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: true });
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      setJoyride({ ...joyride, joyrideDetaljerHarVistOpprettAvtale: true });
    }
  };

  return (
    <Joyride
      locale={locale}
      continuous
      run={!joyride.joyrideDetaljerHarVistOpprettAvtale}
      steps={steps}
      hideCloseButton
      callback={handleJoyrideCallbackOpprettAvtale}
      showSkipButton
      disableScrolling
      styles={styling}
      disableCloseOnEsc={false}
      disableOverlayClose={true}
    />
  );
};
