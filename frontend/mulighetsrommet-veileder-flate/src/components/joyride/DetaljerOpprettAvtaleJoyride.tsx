import { useAtom } from "jotai";
import { JoyrideType } from "mulighetsrommet-api-client";
import Joyride, { ACTIONS, CallBackProps, STATUS } from "react-joyride";
import { useLagreJoyrideForVeileder } from "../../core/api/queries/useLagreJoyrideForVeileder";
import { joyrideAtom } from "../../core/atoms/atoms";
import { opprettAvtaleSteps, useSteps } from "./Steps";
import { locale, styling } from "./config";

interface Props {
  opprettAvtale: boolean;
}

export const DetaljerOpprettAvtaleJoyride = ({ opprettAvtale }: Props) => {
  const [joyride, setJoyride] = useAtom(joyrideAtom);
  useLagreJoyrideForVeileder(
    JoyrideType.HAR_VIST_OPPRETT_AVTALE,
    "joyrideDetaljerHarVistOpprettAvtale",
  );

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
