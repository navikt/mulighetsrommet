import { JoyrideType } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from "react-joyride";
import { logEvent } from "../../core/api/logger";
import { useLagreJoyrideForVeileder } from "../../core/api/queries/useLagreJoyrideForVeileder";
import { useVeilederHarFullfortJoyride } from "../../core/api/queries/useVeilederHarFullfortJoyride";
import { JoyrideKnapp } from "./JoyrideKnapp";
import { oversiktenSteps, useSteps } from "./Steps";
import { locale, styling } from "./config";

interface Props {
  isTableFetched: boolean;
}

export function OversiktenJoyride({ isTableFetched }: Props) {
  const veilederHarKjortJoyrideMutation = useLagreJoyrideForVeileder();
  const { data = false, isLoading } = useVeilederHarFullfortJoyride(JoyrideType.OVERSIKT);
  const [ready, setReady] = useState(!isLoading && !data && isTableFetched);

  useEffect(() => {
    setReady(!isLoading && !data && isTableFetched);
  }, [isLoading, data, isTableFetched]);

  const { steps, stepIndex, setStepIndex } = useSteps(ready, oversiktenSteps);

  const harFullfortJoyride = () =>
    veilederHarKjortJoyrideMutation.mutate({ joyrideType: JoyrideType.OVERSIKT, fullfort: true });

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
        joyrideType: JoyrideType.OVERSIKT,
        fullfort: false,
      });
      setStepIndex(0);
    }

    //resetter joyride når den er ferdig eller man klikker skip
    else if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)) {
      logEvent("mulighetsrommet.joyride", { value: "oversikten", status });
      harFullfortJoyride();
      setStepIndex(0);
      setReady(false);
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      harFullfortJoyride();
      setStepIndex(0);
      setReady(false);
    }
  };

  return (
    <>
      <JoyrideKnapp
        handleClick={() => {
          setReady(true);
          setStepIndex(0);
          logEvent("mulighetsrommet.joyride", { value: "oversikten" });
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
