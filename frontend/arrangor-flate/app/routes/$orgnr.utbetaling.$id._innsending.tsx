import { Heading, Hide, HStack, Link, Stepper, VStack } from "@navikt/ds-react";
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
    <VStack gap="4" width="100%" className="bg-bg-subtle flex-1 px-10 pt-4 pb-10">
      <Link as={ReactRouterLink} to={pathByOrgnr(orgnr).utbetalinger} className="max-w-max">
        <ChevronLeftIcon /> Tilbake til oversikt
      </Link>
      <HStack gap="10" width="100%" justify="center" className="xl:max-w-[1920px] xl:mx-auto">
        <VStack padding="8" flexGrow="2" className="bg-bg-default rounded-lg">
          <Outlet />
        </VStack>
        <Hide below="xl">
          <VStack flexGrow="1">
            <Heading size="medium" spacing level="2" id="stepper-heading">
              Steg
            </Heading>
            <Stepper
              aria-labelledby="stepper-heading"
              activeStep={activeStep}
              onStepChange={setActiveStep}
              interactive={false}
            >
              <Stepper.Step>Innsendingsinformasjon</Stepper.Step>
              <Stepper.Step>Beregning</Stepper.Step>
              <Stepper.Step>Oppsummering</Stepper.Step>
            </Stepper>
          </VStack>
        </Hide>
      </HStack>
    </VStack>
  );
}
