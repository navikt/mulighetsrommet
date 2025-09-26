import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { Box, Hide, Link, Stepper, VStack, VStackProps } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { pathByOrgnr, useOrgnrFromUrl } from "~/utils/navigation";

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
  contentGap?: VStackProps["gap"];
}
export function InnsendingLayout({
  steps,
  activeStep,
  hideStepper,
  children,
  topNavigationLink,
  contentGap,
}: InnsendingLayoutProps) {
  const orgnr = useOrgnrFromUrl();
  const { path, text } = topNavigationLink || {
    path: pathByOrgnr(orgnr).utbetalinger,
    text: "Tilbake til oversikt",
  };

  return (
    <Box width="100%" className="bg-bg-subtle flex-1 px-10 pt-4 pb-10">
      <VStack gap="4" justify="center" className="xl:max-w-[1920px] xl:mx-auto">
        <Link as={ReactRouterLink} to={path} className="max-w-max">
          <ChevronLeftIcon /> {text}
        </Link>
        {steps && steps.length > 0 && (
          <Hide below="sm" hidden={hideStepper}>
            <Stepper aria-label="Steg" activeStep={activeStep || 0} orientation="horizontal">
              {steps.map(({ name, order }) => (
                <Stepper.Step
                  key={name}
                  interactive={false}
                  completed={!!activeStep && activeStep > order}
                >
                  {name}
                </Stepper.Step>
              ))}
            </Stepper>
          </Hide>
        )}
        <VStack padding="8" flexGrow="2" gap={contentGap} className="bg-bg-default rounded-lg">
          {children}
        </VStack>
      </VStack>
    </Box>
  );
}
