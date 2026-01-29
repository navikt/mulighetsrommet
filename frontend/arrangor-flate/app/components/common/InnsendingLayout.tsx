import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { Box, Hide, Link, Stepper, VStack } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { pathTo } from "~/utils/navigation";

interface StepperSteg {
  name: string;
  order: number;
}

interface TopNavigationLink {
  path: string;
  text: string;
}

interface InnsendingLayoutProps {
  children: React.ReactNode | React.ReactNode[];
  steps?: StepperSteg[];
  activeStep?: StepperSteg["order"];
  hideStepper?: boolean;
  topNavigationLink?: TopNavigationLink;
}
export function InnsendingLayout({
  steps,
  activeStep,
  hideStepper,
  children,
  topNavigationLink,
}: InnsendingLayoutProps) {
  const { path, text } = topNavigationLink || {
    path: pathTo.utbetalinger,
    text: "Tilbake til oversikt",
  };

  return (
    <VStack gap="4" justify="center">
      <Link as={ReactRouterLink} to={path} className="max-w-max">
        <ChevronLeftIcon /> {text}
      </Link>
      {steps && steps.length > 0 && (
        <Hide below="sm" hidden={hideStepper}>
          <Stepper aria-label="Steg" activeStep={activeStep || 0} orientation="horizontal">
            {steps.map(({ name }, index) => (
              <Stepper.Step
                key={name}
                interactive={false}
                completed={!!activeStep && activeStep > index + 1}
              >
                {name}
              </Stepper.Step>
            ))}
          </Stepper>
        </Hide>
      )}
      <Box background="bg-default" borderRadius="large" padding="8">
        {children}
      </Box>
    </VStack>
  );
}
