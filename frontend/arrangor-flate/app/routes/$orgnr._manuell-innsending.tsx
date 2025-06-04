import { Heading, Hide, HStack, Link, Stepper, VStack } from "@navikt/ds-react";
import { Outlet, useLocation } from "react-router";
import { internalNavigation } from "~/internal-navigation";
import { Link as ReactRouterLink } from "react-router";
import { useOrgnrFromUrl } from "~/utils";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { useEffect, useState } from "react";

function useStep(path: string) {
  switch (path.split("/").pop()) {
    case "tilsagn":
      return 1;
    case "vedlegg":
      return 2;
    case "utbetalingsinformasjon":
      return 3;
    case "oppsummering":
      return 4;
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
      <Link as={ReactRouterLink} to={internalNavigation(orgnr).utbetalinger} className="max-w-max">
        <ChevronLeftIcon /> Tilbake til oversikt
      </Link>
      <HStack gap="10" width="100%" justify="center" className="xl:max-w-[1920px] xl:mx-auto">
        <VStack padding="8" flexGrow="2" className="bg-bg-default rounded-lg xl:max-w-5xl">
          <Outlet />
        </VStack>
        <Hide below="lg">
          <VStack flexGrow="1">
            <Heading size="medium" spacing level="2" id="stepper-heading">
              Steg
            </Heading>
            <Stepper
              aria-labelledby="Innsendingssteg"
              activeStep={activeStep}
              onStepChange={setActiveStep}
              interactive={false}
            >
              <Stepper.Step>Tilsagn</Stepper.Step>
              <Stepper.Step>Vedlegg</Stepper.Step>
              <Stepper.Step>Utbetalingsinformasjon</Stepper.Step>
              <Stepper.Step>Oppsummering</Stepper.Step>
            </Stepper>
          </VStack>
        </Hide>
      </HStack>
    </VStack>
  );
}
