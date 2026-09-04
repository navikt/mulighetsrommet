import { useLocation, useNavigate } from "react-router";
import { pathTo, useOrgnrFromUrl, UtbetalingInnsendingStep } from "~/utils/navigation";
import { ArrangorflateUtbetalingDto } from "@arrangor-utbetalinger/api-client";

export interface Step {
  name: string;
  path: UtbetalingInnsendingStep;
}

export interface UtbetalingWizard {
  steps: Step[];
  activeStep: number;
  goToNext: (extraState?: Record<string, unknown>) => void;
  goToPrevious: () => void;
  isFirstStep: boolean;
  isLastStep: boolean;
  cancelHref: string;
}

export function useUtbetalingWizard(utbetaling: ArrangorflateUtbetalingDto): UtbetalingWizard {
  const orgnr = useOrgnrFromUrl();
  const navigate = useNavigate();
  const location = useLocation();

  const steps = resolveSteps(utbetaling);
  const currentPath = location.pathname.split("/").pop();
  const currentIndex = steps.findIndex((s) => s.path === currentPath);

  const goToStep = (index: number, extraState?: Record<string, unknown>) => {
    const step = steps[index];
    navigate(pathTo.utbetaling(orgnr, utbetaling.id, step.path), {
      state: {
        updatedAt: utbetaling.updatedAt,
        ...location.state,
        ...extraState,
      },
    });
  };

  return {
    steps,
    activeStep: currentIndex < 0 ? 1 : currentIndex + 1,
    goToNext: (extraState) => goToStep(currentIndex + 1, extraState),
    goToPrevious: () => goToStep(currentIndex - 1),
    isFirstStep: currentIndex === 0,
    isLastStep: currentIndex === steps.length - 1,
    cancelHref: pathTo.utbetalinger,
  };
}

function resolveSteps(utbetaling: ArrangorflateUtbetalingDto): Step[] {
  const steps: Step[] = [{ name: "Innsendingsinformasjon", path: "innsendingsinformasjon" }];

  if (utbetaling.beregning.deltakelser) {
    steps.push({ name: "Deltakere", path: "deltakere" });
  }

  steps.push({ name: "Betalingsinformasjon", path: "betalingsinformasjon" });

  if (utbetaling.beregning.pris.type === "KREVER_REGISTRERING") {
    steps.push({ name: "Vedlegg", path: "vedlegg" });
  }

  steps.push({ name: "Oppsummering", path: "oppsummering" });

  return steps;
}
