import { JoyrideType } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from "react-joyride";
import { logEvent } from "../../core/api/logger";
import { useLagreJoyrideForVeileder } from "../../core/api/queries/useLagreJoyrideForVeileder";
import { useVeilederHarFullfortJoyride } from "../../core/api/queries/useVeilederHarFullfortJoyride";
import styles from "./Joyride.module.scss";
import { JoyrideKnapp } from "./JoyrideKnapp";
import { detaljerSteps, isStep, useSteps } from "./Steps";
import { locale, styling } from "./config";

interface Props {
  opprettAvtale: boolean;
}

export function DetaljerJoyride({ opprettAvtale }: Props) {
  const veilederHarKjortJoyrideMutation = useLagreJoyrideForVeileder();
  const { data = false, isLoading } = useVeilederHarFullfortJoyride(JoyrideType.DETALJER);
  const [ready, setReady] = useState(!isLoading && !data);

  useEffect(() => {
    setReady(!isLoading && !data);
  }, [isLoading, data]);

  const { steps, stepIndex, setStepIndex } = useSteps(ready, detaljerSteps);
  const harFullfortJoyride = () =>
    veilederHarKjortJoyrideMutation.mutate({ joyrideType: JoyrideType.DETALJER, fullfort: true });

  const handleJoyrideCallback = (data: CallBackProps) => {
    const { action, index, status, type } = data;
    const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);

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
      veilederHarKjortJoyrideMutation.mutate({
        fullfort: false,
        joyrideType: JoyrideType.DETALJER,
      });
      setStepIndex(0);
    }

    //resetter joyride når den er ferdig eller man klikker skip
    else if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status)) {
      logEvent("mulighetsrommet.joyride", { value: "detaljer", status });
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
    <>
      <JoyrideKnapp
        handleClick={() => {
          setReady(true);
          logEvent("mulighetsrommet.joyride", { value: "detaljer" });
        }}
        className={styles.joyride_detaljer}
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
