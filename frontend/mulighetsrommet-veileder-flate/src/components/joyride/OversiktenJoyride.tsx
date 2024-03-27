import { JoyrideType } from "mulighetsrommet-api-client";
import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from "react-joyride";
import { useJoyride } from "@/api/queries/useJoyride";
import { JoyrideKnapp } from "./JoyrideKnapp";
import { oversiktenSteps, useSteps } from "./Steps";
import { locale, styling } from "./config";

export function OversiktenJoyride() {
  const { isReady, setIsReady, setHarFullfort } = useJoyride(JoyrideType.OVERSIKT);
  const { steps, stepIndex, setStepIndex } = useSteps(isReady, oversiktenSteps);

  const handleJoyrideCallback = (data: CallBackProps) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    //kjører neste step når man klikker på neste
    if (EVENTS.STEP_AFTER === type) {
      setStepIndex(nextStepIndex);
    }

    //resetter joyride ved error
    if (STATUS.ERROR === status) {
      setHarFullfort(false);
      setStepIndex(0);
    }

    //resetter joyride når den er ferdig eller man klikker skip
    else if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)) {
      setStepIndex(0);
      setHarFullfort(true);
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      setHarFullfort(true);
      setStepIndex(0);
    }
  };

  return (
    <>
      <JoyrideKnapp
        handleClick={() => {
          setIsReady(true);
          setStepIndex(0);
        }}
      />
      <Joyride
        locale={locale}
        continuous
        run={isReady}
        steps={steps}
        hideCloseButton
        callback={handleJoyrideCallback}
        showSkipButton
        stepIndex={stepIndex}
        disableScrolling
        styles={styling}
        disableCloseOnEsc={false}
        disableOverlayClose={true}
      />
    </>
  );
}
