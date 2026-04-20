import {
  Button,
  ErrorSummary,
  FileObject,
  HStack,
  Stepper,
  VStack,
  Link,
  Hide,
  Box,
} from "@navikt/ds-react";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import {
  FieldError,
  OpprettKravDeltakere,
  OpprettKravVeiviserSteg,
  OpprettKravVeiviserStegDto,
  PeriodeType,
} from "api-client";
import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link as ReactRouterLink, MetaFunction, useNavigate } from "react-router";
import {
  pathTo,
  deltakerOversiktLenke,
  useGjennomforingIdFromUrl,
  useOrgnrFromUrl,
} from "~/utils/navigation";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { getEnvironment } from "~/services/environment";
import DeltakereSteg from "~/components/opprett-krav/DeltakereSteg";
import UtbetalingSteg from "~/components/opprett-krav/UtbetalingSteg";
import VedleggSteg from "~/components/opprett-krav/VedleggSteg";
import OppsummeringSteg from "~/components/opprett-krav/OppsummeringSteg";
import InnsendingsinformasjonSteg, {
  validateInnsendingsinformasjon,
} from "~/components/opprett-krav/InnsendingsinformasjonSteg";
import { Laster } from "~/components/common/Laster";
import { useOpprettKravData } from "~/hooks/useOpprettKravData";
import { useOpprettKravDeltakere } from "~/hooks/useOpprettKravDeltakere";
import { useOpprettKrav } from "~/hooks/useOpprettKrav";

interface Step {
  name: string;
  type: OpprettKravVeiviserSteg;
  order: number;
}

const defaultTitle = "Opprett krav om utbetaling";

export const meta: MetaFunction = () => {
  return [{ title: defaultTitle }, { name: "description", content: "Opprett krav om utbetaling" }];
};

export interface OpprettKravFormState {
  periodeStart?: string;
  periodeSlutt?: string;
  periodeType: PeriodeType;
  tilsagnId?: string;
  belop?: string;
  kontonummer?: string;
  kid?: string;
  files: FileObject[];
}

export default function OpprettKravRoute() {
  const gjennomforingId = useGjennomforingIdFromUrl();
  const orgnr = useOrgnrFromUrl();

  return (
    <Suspense fallback={<Laster tekst="Laster data..." size="xlarge" />}>
      <OpprettKravContent orgnr={orgnr} gjennomforingId={gjennomforingId} />
    </Suspense>
  );
}

interface OpprettKravContentProps {
  orgnr: string;
  gjennomforingId: string;
}

function OpprettKravContent({ orgnr, gjennomforingId }: OpprettKravContentProps) {
  const { data, refetch } = useOpprettKravData(orgnr, gjennomforingId);
  const deltakerlisteUrl = deltakerOversiktLenke(getEnvironment());
  const navigate = useNavigate();

  const steps = useMemo(
    () =>
      data.steg.map((steg: OpprettKravVeiviserStegDto) => ({
        name: steg.navn,
        type: steg.type,
        order: steg.order,
      })),
    [data.steg],
  );

  const { innsendingSteg, utbetalingSteg, vedleggSteg } = data;

  const fetchDeltakere = useOpprettKravDeltakere();
  const opprettKrav = useOpprettKrav();

  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [formState, setFormState] = useState<OpprettKravFormState>({
    periodeType: PeriodeType.EKSKLUSIV,
    files: [],
  });
  const [clientErrors, setClientErrors] = useState<FieldError[]>([]);
  const [deltakere, setDeltakere] = useState<OpprettKravDeltakere | null>(null);
  const errors = clientErrors;

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

  const validateUtbetaling = (): boolean => {
    const newErrors: FieldError[] = [];

    if (!formState.belop) {
      newErrors.push({ pointer: "/belop", detail: "Du må fylle ut beløp" });
    } else {
      const belopNumber = Number(formState.belop);
      if (isNaN(belopNumber)) {
        newErrors.push({ pointer: "/belop", detail: "Beløp må være et tall" });
      } else if (!Number.isInteger(belopNumber)) {
        newErrors.push({ pointer: "/belop", detail: "Beløp må være et heltall" });
      }
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
      const innsendingErrors = validateInnsendingsinformasjon({
        data: data.innsendingSteg,
        formState,
      });
      if (innsendingErrors.length > 0) {
        setClientErrors(innsendingErrors);
        return;
      }

      try {
        const result = await fetchDeltakere.mutateAsync({
          orgnr,
          gjennomforingId,
          periodeStart: formState.periodeStart!,
          periodeSlutt: formState.periodeSlutt!,
        });
        setDeltakere(result);
      } catch {
        setClientErrors([{ pointer: "/periode", detail: "Kunne ikke hente deltakere" }]);
        return;
      }
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

  const handleOpprettKrav = async (bekreftelse: boolean) => {
    setClientErrors([]);
    const newErrors: FieldError[] = [];
    const acceptedFiles = formState.files.filter((f) => !f.error);

    if (acceptedFiles.length < vedleggSteg.minAntallVedlegg) {
      newErrors.push({ pointer: "/vedlegg", detail: "Du må legge ved vedlegg" });
    }

    if (!bekreftelse) {
      newErrors.push({
        pointer: "/bekreftelse",
        detail: "Du må bekrefte at opplysningene er korrekte",
      });
    }

    if (!formState.tilsagnId) {
      newErrors.push({ pointer: "/tilsagnId", detail: "Mangler tilsagn" });
    }

    if (newErrors.length > 0) {
      setClientErrors(newErrors);
      return;
    }

    const result = await opprettKrav.mutateAsync({
      orgnr,
      gjennomforingId,
      belop: Number(formState.belop),
      periodeStart: formState.periodeStart!,
      periodeSlutt: formState.periodeSlutt!,
      periodeType: formState.periodeType,
      kidNummer: formState.kid || null,
      vedlegg: acceptedFiles.map((f) => f.file),
    });

    if (result.errors) {
      setClientErrors(result.errors);
    } else if (result.success && result.id) {
      navigate(pathTo.kvittering(orgnr, result.id));
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
            onRevalidate={() => refetch()}
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
            errors={errors}
            goToPreviousStep={goToPreviousStep}
            onSubmit={handleOpprettKrav}
            isSubmitting={opprettKrav.isPending}
          />
        );
      default:
        return null;
    }
  };

  const isLastStep = currentStepIndex === steps.length - 1;
  const isFirstStep = currentStepIndex === 0;

  return (
    <VStack gap="space-4" justify="center">
      <Link as={ReactRouterLink} to={pathTo.tiltaksoversikt} className="max-w-max">
        <ChevronLeftIcon /> Tilbake til tiltaksoversikt
      </Link>
      <Hide below="sm">
        <Stepper aria-label="Steg" activeStep={currentStepIndex + 1} orientation="horizontal">
          {steps.map((step: Step, index: number) => (
            <Stepper.Step key={step.name} interactive={false} completed={currentStepIndex > index}>
              {step.name}
            </Stepper.Step>
          ))}
        </Stepper>
      </Hide>
      <Box background="default" borderRadius="8" padding="space-32">
        <VStack gap="space-8">
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
                <Button
                  type="button"
                  variant="tertiary"
                  onClick={() => navigate(pathTo.tiltaksoversikt)}
                >
                  Avbryt
                </Button>
              ) : (
                <Button type="button" variant="tertiary" onClick={goToPreviousStep}>
                  Tilbake
                </Button>
              )}
              <Button type="button" onClick={goToNextStep} loading={fetchDeltakere.isPending}>
                Neste
              </Button>
            </HStack>
          )}
        </VStack>
      </Box>
    </VStack>
  );
}
