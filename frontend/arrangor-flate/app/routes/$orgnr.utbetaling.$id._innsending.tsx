import { Heading, Link, Stack, Stepper, VStack } from "@navikt/ds-react";
import { Outlet, useLocation } from "react-router";
import { internalNavigation } from "~/internal-navigation";
import { Link as ReactRouterLink } from "react-router";
import { useOrgnrFromUrl } from "~/utils";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { useEffect, useState } from "react";

function useStep(path: string) {
  switch (path.split("/").pop()) {
    case "innsendingsinformasjon":
      return 1;
    case "beregning":
      return 2;
    case "oppsummering":
      return 3;
    case "kvittering":
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
      <Link as={ReactRouterLink} to={internalNavigation(orgnr).utbetalinger}>
        <ChevronLeftIcon /> Tilbake til oversikt
      </Link>
      <Stack
        gap={{ xl: "10" }}
        width="100%"
        justify="center"
        direction={{ xs: "column", xl: "row" }}
        className="xl:max-w-[1920px] xl:mx-auto"
      >
        <VStack padding="8" flexGrow="2" className="bg-bg-default rounded-lg">
          <Outlet />
        </VStack>
        <VStack flexGrow="1" className="xl:hidden">
          <Heading size="medium" spacing level="2" id="stepper-heading" className="max-2xl:hidden">
            Steg
          </Heading>
          <Stepper
            aria-labelledby="Innsendingssteg"
            activeStep={activeStep}
            onStepChange={setActiveStep}
            interactive={false}
            className="max-2xl:hidden"
          >
            <Stepper.Step>Innsendingsinformasjon</Stepper.Step>
            <Stepper.Step>Beregning</Stepper.Step>
            <Stepper.Step>Oppsummering</Stepper.Step>
            <Stepper.Step>Bekreftelse</Stepper.Step>
          </Stepper>
        </VStack>
      </Stack>
    </VStack>
  );
}
