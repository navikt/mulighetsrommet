import { useAtom } from "jotai";
import { JoyrideType } from "mulighetsrommet-api-client";
import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from "react-joyride";
import { useLagreJoyrideForVeileder } from "../../core/api/queries/useLagreJoyrideForVeileder";
import { joyrideAtom } from "../../core/atoms/atoms";
import { JoyrideKnapp } from "./JoyrideKnapp";
import { oversiktenSteps, useSteps } from "./Steps";
import { locale, styling } from "./config";

interface Props {
  isTableFetched: boolean;
}

export function OversiktenJoyride({ isTableFetched }: Props) {
  const [joyride, setJoyride] = useAtom(joyrideAtom);
  useLagreJoyrideForVeileder(JoyrideType.OVERSIKT, "joyrideOversikten");
  const ready = joyride.joyrideOversikten && isTableFetched;

  const { steps, stepIndex, setStepIndex } = useSteps(ready, oversiktenSteps);

  const handleJoyrideCallback = (data: CallBackProps) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    //kjører neste step når man klikker på neste
    if (EVENTS.STEP_AFTER === type) {
      setStepIndex(nextStepIndex);
    }

    //resetter joyride ved error
    if (STATUS.ERROR === status) {
      setJoyride({ ...joyride, joyrideOversikten: true });
      setStepIndex(0);
    }

    //resetter joyride når den er ferdig eller man klikker skip
    else if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)) {
      if (joyride.joyrideOversiktenLastStep === null) {
        setJoyride({ ...joyride, joyrideOversiktenLastStep: true, joyrideOversikten: false });
      } else {
        setJoyride({ ...joyride, joyrideOversikten: false });
      }
      setStepIndex(0);
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      setJoyride({ ...joyride, joyrideOversikten: false });
      setStepIndex(0);
      if (joyride.joyrideOversiktenLastStep === null) {
        setJoyride({ ...joyride, joyrideOversiktenLastStep: true, joyrideOversikten: false });
      }
    }
  };

  return (
    <>
      <JoyrideKnapp
        handleClick={() => {
          setJoyride({ ...joyride, joyrideOversikten: true });
          setStepIndex(0);
        }}
      />
      <Joyride
        locale={locale}
        continuous
        run={ready}
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
