import { JoyrideType } from "mulighetsrommet-api-client";
import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from "react-joyride";
import { useJoyride } from "@/api/queries/useJoyride";
import { JoyrideKnapp } from "./JoyrideKnapp";
import { detaljerSteps, isStep, useSteps } from "./Steps";
import { locale, styling } from "./config";

interface Props {
  opprettAvtale: boolean;
}

export function DetaljerJoyride({ opprettAvtale }: Props) {
  const { isReady, setIsReady, setHarFullfort } = useJoyride(JoyrideType.DETALJER);
  const { steps, stepIndex, setStepIndex } = useSteps(isReady, detaljerSteps);

  const handleJoyrideCallback = (data: CallBackProps) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    // Hopper over steg hvis target ikke eksisterer
    if (EVENTS.TARGET_NOT_FOUND === type) {
      setStepIndex(nextStepIndex);
    }

    //kjører neste step når man klikker på neste
    if (EVENTS.STEP_AFTER === type) {
      setStepIndex(nextStepIndex);
    }

    if (!opprettAvtale) {
      //hopper over steget med opprett avtale for at den skal kjøre videre til neste steg
      if (isStep(data.step, "opprett-avtale")) {
        setStepIndex(nextStepIndex);
      }
    }

    //resetter joyride ved error
    if (STATUS.ERROR === status) {
      setHarFullfort(false);
      setStepIndex(0);
    }

    //resetter joyride når den er ferdig eller man klikker skip
    else if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)) {
      setHarFullfort(true);
      setStepIndex(0);
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
          setStepIndex(0);
          setIsReady(true);
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
