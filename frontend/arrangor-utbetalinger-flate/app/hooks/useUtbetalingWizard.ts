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
  goToNext: () => void;
  goToPrevious: () => void;
  isFirstStep: boolean;
  isLastStep: boolean;
}

export function useUtbetalingWizard(utbetaling: ArrangorflateUtbetalingDto): UtbetalingWizard {
  const orgnr = useOrgnrFromUrl();
  const navigate = useNavigate();
  const location = useLocation();

  const steps = resolveSteps(utbetaling);
  const currentPath = location.pathname.split("/").pop();
  const currentIndex = steps.findIndex((s) => s.path === currentPath);

  const goToStep = (index: number) => {
    const step = steps[index];
    navigate(pathTo.utbetaling(orgnr, utbetaling.id, step.path), {
      state: {
        updatedAt: utbetaling.updatedAt,
      },
    });
  };

  return {
    steps,
    activeStep: currentIndex < 0 ? 1 : currentIndex + 1,
    goToNext: () => goToStep(currentIndex + 1),
    goToPrevious: () => goToStep(currentIndex - 1),
    isFirstStep: currentIndex === 0,
    isLastStep: currentIndex === steps.length - 1,
  };
}

function resolveSteps(utbetaling: ArrangorflateUtbetalingDto): Step[] {
  return utbetaling.kanViseBeregning
    ? [
        { name: "Innsendingsinformasjon", path: "innsendingsinformasjon" },
        { name: "Beregning", path: "beregning" },
        { name: "Oppsummering", path: "oppsummering" },
      ]
    : [
        { name: "Innsendingsinformasjon", path: "innsendingsinformasjon" },
        { name: "Oppsummering", path: "oppsummering" },
      ];
}
