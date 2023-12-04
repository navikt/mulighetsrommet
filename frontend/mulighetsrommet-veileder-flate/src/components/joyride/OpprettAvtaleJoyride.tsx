import { JoyrideType } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from "react-joyride";
import { logEvent } from "../../core/api/logger";
import { useLagreJoyrideForVeileder } from "../../core/api/queries/useLagreJoyrideForVeileder";
import { useVeilederHarFullfortJoyride } from "../../core/api/queries/useVeilederHarFullfortJoyride";
import { opprettAvtaleSteps, useSteps } from "./Steps";
import { locale, styling } from "./config";

export function OpprettAvtaleJoyride() {
  const veilederHarKjortJoyrideMutation = useLagreJoyrideForVeileder();
  const { data = false, isLoading } = useVeilederHarFullfortJoyride(
    JoyrideType.HAR_VIST_OPPRETT_AVTALE,
  );
  const [ready, setReady] = useState(!isLoading && !data);

  useEffect(() => {
    setReady(!isLoading && !data);
  }, [isLoading, data]);

  const { steps, stepIndex, setStepIndex } = useSteps(ready, opprettAvtaleSteps);
  const harFullfortJoyride = () =>
    veilederHarKjortJoyrideMutation.mutate({
      joyrideType: JoyrideType.HAR_VIST_OPPRETT_AVTALE,
      fullfort: true,
    });

  const handleJoyrideCallback = (data: CallBackProps) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

    //kjører neste step når man klikker på neste
    if (EVENTS.STEP_AFTER === type) {
      setStepIndex(nextStepIndex);
    }

    //resetter joyride ved error
    if (STATUS.ERROR === status) {
      veilederHarKjortJoyrideMutation.mutate({
        fullfort: false,
        joyrideType: JoyrideType.HAR_VIST_OPPRETT_AVTALE,
      });
      setStepIndex(0);
    }

    //resetter joyride når den er ferdig eller man klikker skip
    else if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)) {
      logEvent("mulighetsrommet.joyride", { value: "opprettAvtale", status });
      harFullfortJoyride();
      setStepIndex(0);
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      harFullfortJoyride();
      setStepIndex(0);
    }
  };

  return (
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
  );
}
