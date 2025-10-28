import { LoaderFunction, Outlet, useLoaderData } from "react-router";
import { InnsendingLayout } from "~/components/common/InnsendingLayout";
import { getOrgnrGjennomforingIdFrom, pathByOrgnr, useOrgnrFromUrl } from "~/utils/navigation";
import {
  ArrangorflateService,
  OpprettKravVeiviserSteg,
  OpprettKravVeiviserStegDto,
} from "api-client";
import { apiHeaders } from "~/auth/auth.server";
import { problemDetailResponse } from "~/utils/validering";

interface Step {
  name: string;
  path: string;
  order: number;
}

const defaultTitle = "Opprett krav om utbetaling";

export function getStepTitle(routeMatches: { loaderData: unknown }[]) {
  const loaderData = routeMatches
    .map((match) => match.loaderData)
    .find(isDriftstilskuddRootLoaderData);
  if (loaderData) {
    const stepIndex = loaderData.steps.indexOf(loaderData.activeStep!);
    const numOfSteps = loaderData.steps.length;
    return `Steg ${stepIndex + 1} av ${numOfSteps}: ${loaderData.activeStep.name} - ${defaultTitle}`;
  }
  return defaultTitle;
}

function getPathFromSteg(step: OpprettKravVeiviserSteg): string {
  switch (step) {
    case OpprettKravVeiviserSteg.INFORMASJON:
      return "innsendingsinformasjon";
    case OpprettKravVeiviserSteg.DELTAKERLISTE:
      return "deltakere";
    case OpprettKravVeiviserSteg.UTBETALING:
      return "utbetaling";
    case OpprettKravVeiviserSteg.VEDLEGG:
      return "vedlegg";
    case OpprettKravVeiviserSteg.OPPSUMMERING:
      return "oppsummering";
  }
}

export interface OpprettKravLoaderData {
  type: "driftstilskudd";
  steps: Step[];
  activeStep: Step;
}

export function isDriftstilskuddRootLoaderData(obj: unknown): obj is OpprettKravLoaderData {
  return typeof obj === "object" && obj !== null && "type" in obj && obj.type === "driftstilskudd";
}

export const loader: LoaderFunction = async ({
  request,
  params,
}): Promise<OpprettKravLoaderData> => {
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);

  const [{ data: veiviserMeta, error: veiviserMetaError }] = await Promise.all([
    ArrangorflateService.getOpprettKravVeiviser({
      path: { orgnr, gjennomforingId },
      headers: await apiHeaders(request),
    }),
  ]);

  if (veiviserMetaError) {
    throw problemDetailResponse(veiviserMetaError);
  }

  const steps = veiviserMeta.steg.map((steg: OpprettKravVeiviserStegDto) => ({
    name: steg.navn,
    path: getPathFromSteg(steg.type),
    order: steg.order,
  }));

  const activeStep = getActiveStep(steps, new URL(request.url).pathname);

  return { type: "driftstilskudd", steps, activeStep };
};

function getActiveStep(steps: Step[], path: string) {
  const [stepPath] = path.split("/").slice(-1);
  return steps.find(({ path }) => path === stepPath)!;
}

export default function OpprettKravLayout() {
  const { steps, activeStep } = useLoaderData<OpprettKravLoaderData>();
  const orgnr = useOrgnrFromUrl();

  const topNavigationLink = {
    path: pathByOrgnr(orgnr).opprettKrav.tiltaksOversikt,
    text: "Tilbake til tiltaksoversikt",
  };

  const activeStepPosition = steps.indexOf(activeStep) + 1;

  return (
    <InnsendingLayout
      steps={steps}
      activeStep={activeStepPosition}
      hideStepper={activeStepPosition === 0}
      topNavigationLink={topNavigationLink}
    >
      <Outlet />
    </InnsendingLayout>
  );
}
