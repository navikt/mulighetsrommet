import { Box, Heading, Hide, HStack, Link, Stepper, VStack } from "@navikt/ds-react";
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
    case "utbetalingsinformasjon":
      return 2;
    case "oppsummering":
      return 3;
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
      <VStack gap="4" className="4xl:max-w-5xl xl:mx-auto">
        <Link
          as={ReactRouterLink}
          to={internalNavigation(orgnr).utbetalinger}
          className="max-w-max"
        >
          <ChevronLeftIcon /> Tilbake til oversikt
        </Link>
        <Heading size="large" spacing level="2">
          Manuelt utbetalingskrav
        </Heading>
        <HStack gap="10" width="100%" justify="center" wrap={false}>
          <VStack padding="8" flexGrow="2" className="bg-bg-default rounded-lg ">
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
                <Stepper.Step>Utbetalingsinformasjon</Stepper.Step>
                <Stepper.Step>Oppsummering</Stepper.Step>
              </Stepper>
            </VStack>
          </Hide>
        </HStack>
      </VStack>
    </Box>
  );
}
