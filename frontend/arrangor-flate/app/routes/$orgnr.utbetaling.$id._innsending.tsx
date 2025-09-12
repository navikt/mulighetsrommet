import { Box, Hide, HStack, Link, Stepper, VStack } from "@navikt/ds-react";
import { Outlet, useLocation } from "react-router";
import { Link as ReactRouterLink } from "react-router";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { useEffect, useState } from "react";
import { useOrgnrFromUrl, pathByOrgnr } from "~/utils/navigation";

function useStep(path: string): number {
  switch (path.split("/").pop()) {
    case "innsendingsinformasjon":
      return 1;
    case "beregning":
      return 2;
    case "oppsummering":
      return 3;
    case undefined:
    default:
      return 1;
  }
}

export default function UtbetalingLayout() {
  const orgnr = useOrgnrFromUrl();
  const location = useLocation();
  const step = useStep(location.pathname);
  const [activeStep, setActiveStep] = useState(step);

  useEffect(() => {
    setActiveStep(step);
  }, [step]);

  return (
    <Box width="100%" className="bg-bg-subtle flex-1 px-10 pt-4 pb-10">
      <VStack gap="4" justify="center" className="xl:max-w-[1920px] xl:mx-auto">
        <Link as={ReactRouterLink} to={pathByOrgnr(orgnr).utbetalinger} className="max-w-max">
          <ChevronLeftIcon /> Tilbake til oversikt
        </Link>
        <Hide below="sm">
          <Stepper
            aria-labelledby="stepper-heading"
            aria-label="Steg"
            activeStep={activeStep}
            onStepChange={setActiveStep}
            orientation="horizontal"
          >
            <Stepper.Step interactive={false} completed={activeStep > 1}>
              Innsendingsinformasjon
            </Stepper.Step>
            <Stepper.Step interactive={false} completed={activeStep > 2}>
              Beregning
            </Stepper.Step>
            <Stepper.Step interactive={false}>Oppsummering</Stepper.Step>
          </Stepper>
        </Hide>
        <VStack padding="8" flexGrow="2" className="bg-bg-default rounded-lg">
          <Outlet />
        </VStack>
      </VStack>
    </Box>
  );
}
