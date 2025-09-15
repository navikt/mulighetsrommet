import { Outlet, useLocation } from "react-router";
import { useEffect, useState } from "react";
import { InnsendingLayout } from "~/components/common/InnsendingLayout";

const steps = [
  { name: "Innsendingsinformasjon", path: "innsendingsinformasjon", order: 1 },
  { name: "Utbetalingsinformasjon", path: "utbetaling", order: 2 },
  { name: "Oppsummering", path: "oppsummering", order: 3 },
];

function useStep(path: string) {
  const [type, stepPath] = path.split("/").slice(-2);
  if (type === "info") {
    return 0;
  }
  return steps.find(({ path }) => path === stepPath)?.order || 1;
}

export default function UtbetalingLayout() {
  const location = useLocation();
  const step = useStep(location.pathname);
  const [activeStep, setActiveStep] = useState(step);

  useEffect(() => {
    setActiveStep(step);
  }, [step]);

  return (
    <InnsendingLayout steps={steps} activeStep={activeStep} hideStepper={activeStep === 0}>
      <Outlet />
    </InnsendingLayout>
  );
}
