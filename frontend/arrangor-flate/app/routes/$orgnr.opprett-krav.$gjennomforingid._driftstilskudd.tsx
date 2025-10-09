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

export interface LoaderData {
  steps: Step[];
  activeStep: Step | undefined;
}

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
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

  return { steps, activeStep };
};

function getActiveStep(steps: Step[], path: string) {
  const [stepPath] = path.split("/").slice(-1);
  return steps.find(({ path }) => path === stepPath);
}

export default function UtbetalingLayout() {
  const { steps, activeStep } = useLoaderData<LoaderData>();
  const orgnr = useOrgnrFromUrl();

  const topNavigationLink = {
    path: pathByOrgnr(orgnr).opprettKrav.tiltaksOversikt,
    text: "Tilbake til tiltaksoversikt",
  };

  const activeStepOrder = activeStep?.order || 0;

  return (
    <InnsendingLayout
      steps={steps}
      activeStep={activeStepOrder || 0}
      hideStepper={activeStepOrder === 0}
      topNavigationLink={topNavigationLink}
    >
      <Outlet />
    </InnsendingLayout>
  );
}
