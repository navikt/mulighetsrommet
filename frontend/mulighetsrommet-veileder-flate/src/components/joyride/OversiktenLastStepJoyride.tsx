import { useAtom } from "jotai";
import { JoyrideType } from "mulighetsrommet-api-client";
import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from "react-joyride";
import { useLagreJoyrideForVeileder } from "../../core/api/queries/useLagreJoyrideForVeileder";
import { joyrideAtom } from "../../core/atoms/atoms";
import { oversiktenLastStep, useSteps } from "./Steps";
import { locale, styling } from "./config";

export const OversiktenLastStepJoyride = () => {
  const [joyride, setJoyride] = useAtom(joyrideAtom);
  useLagreJoyrideForVeileder(JoyrideType.OVERSIKTEN_LAST_STEP, "joyrideOversiktenLastStep");

  const { steps } = useSteps(joyride.joyrideOversiktenLastStep, oversiktenLastStep);

  const handleJoyrideCallbackLastStep = (data: CallBackProps) => {
    const { status, type, action } = data;
    if (
      ([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status) ||
      EVENTS.TOOLTIP === type
    ) {
      setJoyride({ ...joyride, joyrideOversiktenLastStep: false });
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      setJoyride({ ...joyride, joyrideOversiktenLastStep: false });
    }
  };

  return (
    <Joyride
      locale={locale}
      run={joyride.joyrideOversiktenLastStep === true}
      steps={steps}
      callback={handleJoyrideCallbackLastStep}
      disableScrolling
      hideCloseButton
      styles={styling}
      disableCloseOnEsc={false}
      disableOverlayClose={true}
    />
  );
};
