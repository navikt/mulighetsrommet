import { Outlet, useLocation } from "react-router";
import { useEffect, useState } from "react";
import { InnsendingLayout } from "~/components/common/InnsendingLayout";
import { pathByOrgnr, useOrgnrFromUrl } from "~/utils/navigation";

const steps = [
  { name: "Innsendingsinformasjon", path: "innsendingsinformasjon", order: 1 },
  { name: "Utbetalingsinformasjon", path: "utbetaling", order: 2 },
  { name: "Vedlegg", path: "vedlegg", order: 3 },
  { name: "Oppsummering", path: "oppsummering", order: 4 },
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
  const orgnr = useOrgnrFromUrl();

  useEffect(() => {
    setActiveStep(step);
  }, [step]);

  const topNavigationLink = {
    path: pathByOrgnr(orgnr).opprettKrav.tiltaksOversikt,
    text: "Tilbake til tiltaksoversikt",
  };

  return (
    <InnsendingLayout
      steps={steps}
      activeStep={activeStep}
      hideStepper={activeStep === 0}
      topNavigationLink={topNavigationLink}
    >
      <Outlet />
    </InnsendingLayout>
  );
}
