import { Button, ErrorSummary, FileObject, HStack, VStack } from "@navikt/ds-react";
import {
  ArrangorflateService,
  FieldError,
  OpprettKravData,
  OpprettKravDeltakere,
  OpprettKravVeiviserSteg,
  OpprettKravVeiviserStegDto,
} from "api-client";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  ActionFunctionArgs,
  Form,
  LoaderFunction,
  MetaFunction,
  redirect,
  useActionData,
  useFetcher,
  useLoaderData,
  useRevalidator,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { isValidationError, problemDetailResponse } from "~/utils/validering";
import { getOrgnrGjennomforingIdFrom, pathTo, deltakerOversiktLenke } from "~/utils/navigation";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { isLaterOrSameDay, parseDate } from "@mr/frontend-common/utils/date";
import { getEnvironment } from "~/services/environment";
import {
  FileUpload as MjacksonFileUpload,
  FileUploadHandler,
  parseFormData,
} from "@mjackson/form-data-parser";
import DeltakereSteg from "~/components/opprett-krav/DeltakereSteg";
import UtbetalingSteg from "~/components/opprett-krav/UtbetalingSteg";
import VedleggSteg from "~/components/opprett-krav/VedleggSteg";
import OppsummeringSteg from "~/components/opprett-krav/OppsummeringSteg";
import InnsendingsinformasjonSteg from "~/components/opprett-krav/InnsendingsinformasjonSteg";
import { InnsendingLayout } from "~/components/common/InnsendingLayout";

interface Step {
  name: string;
  type: OpprettKravVeiviserSteg;
  order: number;
}

const defaultTitle = "Opprett krav om utbetaling";

export const meta: MetaFunction<typeof loader> = ({ data }) => {
  const loaderData = data as LoaderData | undefined;
  if (loaderData?.activeStep) {
    const stepIndex = loaderData.steps.findIndex(
      (s: Step) => s.type === loaderData.activeStep.type,
    );
    const numOfSteps = loaderData.steps.length;
    return [
      {
        title: `Steg ${stepIndex + 1} av ${numOfSteps}: ${loaderData.activeStep.name} - ${defaultTitle}`,
      },
      { name: "description", content: "Opprett krav om utbetaling" },
    ];
  }
  return [{ title: defaultTitle }];
};

interface LoaderData {
  orgnr: string;
  gjennomforingId: string;
  steps: Step[];
  activeStep: Step;
  data: OpprettKravData;
  deltakerlisteUrl: string;
}

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);
  const deltakerlisteUrl = deltakerOversiktLenke(getEnvironment());

  const [{ data, error: opprettKravDataError }] = await Promise.all([
    ArrangorflateService.getOpprettKravData({
      path: { orgnr, gjennomforingId },
      headers: await apiHeaders(request),
    }),
  ]);

  if (opprettKravDataError) throw problemDetailResponse(opprettKravDataError);

  const steps = data.steg.map((steg: OpprettKravVeiviserStegDto) => ({
    name: steg.navn,
    type: steg.type,
    order: steg.order,
  }));

  const activeStep = steps[0];

  return {
    orgnr,
    gjennomforingId,
    steps,
    activeStep,
    data,
    deltakerlisteUrl,
  };
};

const uploadHandler: FileUploadHandler = async (fileUpload: MjacksonFileUpload) => {
  if (fileUpload.fieldName === "vedlegg" && fileUpload.name.endsWith(".pdf")) {
    const bytes = await fileUpload.bytes();
    return new File([bytes], fileUpload.name, { type: fileUpload.type });
  }
};

interface ActionData {
  errors?: FieldError[];
  intent?: string;
  deltakere?: OpprettKravDeltakere;
}

export async function action({
  request,
  params,
}: ActionFunctionArgs): Promise<ActionData | Response> {
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);

  const formData = await parseFormData(request, { maxFileSize: 10 * 1024 * 1024 }, uploadHandler);

  const intent = formData.get("intent")?.toString();

  if (intent === "cancel") {
    return redirect(pathTo.tiltaksoversikt);
  }

  if (intent === "fetch-deltakere") {
    const periodeStart = formData.get("periodeStart")?.toString();
    const periodeSlutt = formData.get("periodeSlutt")?.toString();

    if (!periodeStart || !periodeSlutt) {
      return { errors: [{ pointer: "/periode", detail: "Periode er ikke valgt" }], intent };
    }

    const { data: deltakere, error } = await ArrangorflateService.getOpprettKravDeltakere({
      path: { orgnr, gjennomforingId },
      query: { periodeStart, periodeSlutt },
      headers: await apiHeaders(request),
    });

    if (error) throw problemDetailResponse(error);
    return { deltakere, intent };
  }

  if (intent === "submit") {
    const errors: FieldError[] = [];
    const vedlegg = formData.getAll("vedlegg") as File[];
    const bekreftelse = formData.get("bekreftelse");
    const belop = Number(formData.get("belop"));
    const periodeStart = formData.get("periodeStart")?.toString();
    const periodeSlutt = formData.get("periodeSlutt")?.toString();
    const kidNummer = formData.get("kid")?.toString();
    const tilsagnId = formData.get("tilsagnId")?.toString();
    const minAntallVedlegg = parseInt(formData.get("minAntallVedlegg")?.toString() || "0");

    if (vedlegg.length < minAntallVedlegg) {
      errors.push({ pointer: "/vedlegg", detail: "Du må legge ved vedlegg" });
    }

    if (!bekreftelse) {
      errors.push({
        pointer: "/bekreftelse",
        detail: "Du må bekrefte at opplysningene er korrekte",
      });
    }

    if (!tilsagnId) {
      errors.push({ pointer: "/tilsagnId", detail: "Mangler tilsagn" });
    }

    if (errors.length > 0) {
      return { errors, intent };
    }

    const { error, data } = await ArrangorflateService.postOpprettKrav({
      path: { orgnr, gjennomforingId },
      body: {
        belop,
        tilsagnId: tilsagnId!,
        periodeStart: periodeStart!,
        periodeSlutt: periodeSlutt!,
        kidNummer: kidNummer || null,
        vedlegg,
      },
      headers: await apiHeaders(request),
    });

    if (error) {
      if (isValidationError(error)) {
        return { errors: error.errors, intent };
      }
      throw problemDetailResponse(error);
    }

    return redirect(pathTo.kvittering(orgnr, data.id));
  }

  return { intent };
}

export interface OpprettKravFormState {
  periodeStart?: string;
  periodeSlutt?: string;
  periodeInklusiv: boolean;
  tilsagnId?: string;
  belop?: string;
  kontonummer?: string;
  kid?: string;
  files: FileObject[];
}

export default function OpprettKravRoute() {
  const { steps, data, deltakerlisteUrl } = useLoaderData<LoaderData>();
  const { innsendingSteg, utbetalingSteg, vedleggSteg } = data;

  const actionData = useActionData<ActionData>();
  const fetcher = useFetcher<ActionData>();
  const revalidator = useRevalidator();

  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [formState, setFormState] = useState<OpprettKravFormState>({
    periodeInklusiv: false,
    files: [],
  });
  const [clientErrors, setClientErrors] = useState<FieldError[]>([]);
  const deltakere = fetcher.data?.deltakere ?? null;
  const errors = clientErrors.length
    ? clientErrors
    : (actionData?.errors ?? fetcher.data?.errors ?? []);

  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const hasError = errors.length > 0;

  const currentStep = steps[currentStepIndex];

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [hasError]);

  const updateFormState = useCallback((updates: Partial<OpprettKravFormState>) => {
    setFormState((prev) => ({ ...prev, ...updates }));
  }, []);

  const validateInnsendingsinformasjon = (): boolean => {
    const newErrors: FieldError[] = [];

    switch (innsendingSteg.datoVelger.type) {
      case "DatoVelgerRange": {
        if (!formState.periodeStart) {
          newErrors.push({ pointer: "/periodeStart", detail: "Du må fylle ut fra dato" });
        }
        if (!formState.periodeSlutt) {
          newErrors.push({ pointer: "/periodeSlutt", detail: "Du må fylle ut til dato" });
        }
        break;
      }
      case "DatoVelgerSelect": {
        if (!formState.periodeStart) {
          newErrors.push({ pointer: "/periodeStart", detail: "Du må velge en periode" });
        }
        if (
          formState.periodeStart &&
          isLaterOrSameDay(parseDate(formState.periodeStart), parseDate(formState.periodeSlutt))
        ) {
          newErrors.push({
            pointer: "/periodeSlutt",
            detail: "Periodeslutt må være etter periodestart",
          });
        }
        break;
      }
      case undefined:
        throw Error("undefined datoVelgerType");
    }

    if (!formState.tilsagnId) {
      newErrors.push({
        pointer: "/tilsagnId",
        detail: "Kan ikke opprette utbetalingskrav uten gyldig tilsagn",
      });
    }

    setClientErrors(newErrors);
    return newErrors.length === 0;
  };

  const validateUtbetaling = (): boolean => {
    const newErrors: FieldError[] = [];

    if (!formState.belop) {
      newErrors.push({ pointer: "/belop", detail: "Du må fylle ut beløp" });
    }
    if (!formState.kontonummer) {
      newErrors.push({ pointer: "/kontonummer", detail: "Fant ikke kontonummer" });
    }

    setClientErrors(newErrors);
    return newErrors.length === 0;
  };

  const validateVedlegg = (): boolean => {
    const newErrors: FieldError[] = [];
    const acceptedFiles = formState.files.filter((f) => !f.error);

    if (acceptedFiles.length < vedleggSteg.minAntallVedlegg) {
      newErrors.push({ pointer: "/vedlegg", detail: "Du må legge ved vedlegg" });
    }

    setClientErrors(newErrors);
    return newErrors.length === 0;
  };

  const goToNextStep = async () => {
    setClientErrors([]);

    if (currentStep.type === OpprettKravVeiviserSteg.INFORMASJON) {
      if (!validateInnsendingsinformasjon()) return;

      // Fetch deltakere for next step
      const formData = new FormData();
      formData.append("intent", "fetch-deltakere");
      formData.append("periodeStart", formState.periodeStart!);
      formData.append("periodeSlutt", formState.periodeSlutt!);
      fetcher.submit(formData, { method: "post" });
    }

    if (currentStep.type === OpprettKravVeiviserSteg.UTBETALING) {
      if (!validateUtbetaling()) return;
    }

    if (currentStep.type === OpprettKravVeiviserSteg.VEDLEGG) {
      if (!validateVedlegg()) return;
    }

    if (currentStepIndex < steps.length - 1) {
      setCurrentStepIndex(currentStepIndex + 1);
    }
  };

  const goToPreviousStep = () => {
    setClientErrors([]);
    if (currentStepIndex > 0) {
      setCurrentStepIndex(currentStepIndex - 1);
    }
  };

  const renderCurrentStep = () => {
    switch (currentStep.type) {
      case OpprettKravVeiviserSteg.INFORMASJON:
        return (
          <InnsendingsinformasjonSteg
            data={innsendingSteg}
            formState={formState}
            updateFormState={updateFormState}
            errors={errors}
          />
        );
      case OpprettKravVeiviserSteg.DELTAKERLISTE:
        return <DeltakereSteg deltakere={deltakere} deltakerlisteUrl={deltakerlisteUrl} />;
      case OpprettKravVeiviserSteg.UTBETALING:
        return (
          <UtbetalingSteg
            data={utbetalingSteg}
            formState={formState}
            updateFormState={updateFormState}
            errors={errors}
            onRevalidate={() => revalidator.revalidate()}
          />
        );
      case OpprettKravVeiviserSteg.VEDLEGG:
        return (
          <VedleggSteg
            data={vedleggSteg}
            formState={formState}
            updateFormState={updateFormState}
            errors={errors}
          />
        );
      case OpprettKravVeiviserSteg.OPPSUMMERING:
        return (
          <OppsummeringSteg
            innsendingsInformasjon={innsendingSteg.definisjonsListe}
            formState={formState}
            vedleggInfo={vedleggSteg}
            errors={errors}
            goToPreviousStep={goToPreviousStep}
          />
        );
      default:
        return null;
    }
  };

  const isLastStep = currentStepIndex === steps.length - 1;
  const isFirstStep = currentStepIndex === 0;

  return (
    <InnsendingLayout
      steps={steps}
      activeStep={currentStepIndex + 1}
      hideStepper={false}
      topNavigationLink={{ path: pathTo.tiltaksoversikt, text: "Tilbake til tiltaksoversikt" }}
    >
      <VStack gap="space-16">
        {renderCurrentStep()}

        {hasError && (
          <ErrorSummary ref={errorSummaryRef}>
            {errors.map((error) => (
              <ErrorSummary.Item
                href={`#${jsonPointerToFieldPath(error.pointer)}`}
                key={jsonPointerToFieldPath(error.pointer)}
              >
                {error.detail}
              </ErrorSummary.Item>
            ))}
          </ErrorSummary>
        )}

        {!isLastStep && (
          <HStack gap="space-8">
            {isFirstStep ? (
              <Form method="post">
                <Button type="submit" variant="tertiary" name="intent" value="cancel">
                  Avbryt
                </Button>
              </Form>
            ) : (
              <Button type="button" variant="tertiary" onClick={goToPreviousStep}>
                Tilbake
              </Button>
            )}
            <Button type="button" onClick={goToNextStep} loading={fetcher.state === "submitting"}>
              Neste
            </Button>
          </HStack>
        )}
      </VStack>
    </InnsendingLayout>
  );
}
