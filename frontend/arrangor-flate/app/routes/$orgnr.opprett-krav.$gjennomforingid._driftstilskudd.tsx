import { LoaderFunction, Outlet, useLocation, useLoaderData } from "react-router";
import { useEffect, useState } from "react";
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

interface LoaderData {
  steps: Step[];
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

  return { steps };
};

function useStep(steps: Step[], path: string) {
  const [stepPath] = path.split("/").slice(-1);
  return steps.find(({ path }) => path === stepPath)?.order || 1;
}

export default function UtbetalingLayout() {
  const { steps } = useLoaderData<LoaderData>();
  const location = useLocation();
  const step = useStep(steps, location.pathname);
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
