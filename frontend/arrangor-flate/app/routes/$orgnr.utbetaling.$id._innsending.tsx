import { Outlet, useLocation } from "react-router";
import { useEffect, useState } from "react";
import { InnsendingLayout } from "~/components/common/InnsendingLayout";

const steps = [
  { name: "Innsendingsinformasjon", path: "innsendingsinformasjon", order: 1 },
  { name: "Beregning", path: "beregning", order: 2 },
  { name: "Oppsummering", path: "oppsummering", order: 3 },
];

function useStep(path: string): number {
  const stepPath = path.split("/").pop();
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
    <InnsendingLayout steps={steps} activeStep={activeStep}>
      <Outlet />
    </InnsendingLayout>
  );
}
