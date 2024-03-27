import { JoyrideType } from "mulighetsrommet-api-client";
import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from "react-joyride";
import { useJoyride } from "@/api/queries/useJoyride";
import { opprettAvtaleSteps, useSteps } from "./Steps";
import { locale, styling } from "./config";

interface Props {
  opprettAvtale: boolean;
}

export function OpprettAvtaleJoyride({ opprettAvtale }: Props) {
  const { harFullfort: harFullfortDetaljer } = useJoyride(JoyrideType.DETALJER);
  const { isReady, harFullfort, setHarFullfort } = useJoyride(JoyrideType.HAR_VIST_OPPRETT_AVTALE);
  const { steps, stepIndex, setStepIndex } = useSteps(isReady, opprettAvtaleSteps);

  if (!harFullfortDetaljer && opprettAvtale) return null;

  if (harFullfort) return null;

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
  );
}
