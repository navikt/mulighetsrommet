import { useOpprettTilskuddBehandling } from "@/api/tilskudd-behandling/mutations";
import { TabWithErrorBorder } from "@/components/skjema/TabWithErrorBorder";
import { ValideringsfeilOppsummering } from "@/components/skjema/ValideringsfeilOppsummering";
import { defaultVedtakRequest } from "@/components/tilskudd-behandling/defaultVedtakRequest";
import { SaksopplysningerForm } from "@/components/tilskudd-behandling/Saksopplysninger";
import { VedtakForm } from "@/components/tilskudd-behandling/VedtakForm";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { TilskuddBehandlingRequest, ValidationError } from "@tiltaksadministrasjon/api-client";
import { Button, HStack } from "@navikt/ds-react";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router";
import { v4 } from "uuid";
import {
  TilskuddBehandlingLayout,
  TilskuddBehandlingTab,
} from "@/components/tilskudd-behandling/TilskuddBehandlingLayout";
import { usePotentialTilskuddBehandling } from "@/api/tilskudd-behandling/useTilskuddBehandling";
import { addDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";

interface Tab {
  key: TilskuddBehandlingTab;
  label: string;
}

const tabs: Tab[] = [
  { key: "saksopplysninger", label: "Saksopplysninger" },
  { key: "vedtak", label: "Vedtak" },
];

export function TilskuddBehandlingFormPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { behandlingId } = useParams();
  const { data: behandling } = usePotentialTilskuddBehandling(behandlingId ?? null);
  const [currentTab, setCurrentTab] = useState<TilskuddBehandlingTab>(tabs[0].key);
  const navigate = useNavigate();
  const mutation = useOpprettTilskuddBehandling(gjennomforingId);

  const form = useForm<TilskuddBehandlingRequest>({
    defaultValues: behandling
      ? {
          id: behandling.id,
          gjennomforingId: behandling.gjennomforingId,
          periodeStart: yyyyMMddFormatting(behandling.periode.start),
          periodeSlutt: yyyyMMddFormatting(addDuration(behandling.periode.slutt, { days: 1 })),
          soknadJournalpostId: behandling.soknadJournalpostId,
          kostnadssted: behandling.kostnadssted,
          soknadDato: behandling.soknadDato,
          vedtak: behandling.vedtak.map((v) => ({
            id: v.id,
            tilskuddOpplaeringType: v.tilskuddOpplaeringType,
            soknadBelop: {
              belop: v.soknadBelop,
              valuta: v.soknadValuta,
            },
            vedtakResultat: v.vedtakResultat,
            kommentarVedtaksbrev: v.kommentarVedtaksbrev,
            utbetalingMottaker: v.utbetalingMottaker,
          })),
        }
      : {
          id: v4(),
          gjennomforingId,
          periodeSlutt: null,
          periodeStart: null,
          soknadJournalpostId: null,
          kostnadssted: null,
          soknadDato: null,
          vedtak: [defaultVedtakRequest],
        },
    mode: "onBlur",
  });

  const {
    handleSubmit,
    setError,
    formState: { errors },
  } = form;

  const currentTabIndex = tabs.findIndex((tab) => tab.key === currentTab);
  const isFirstTab = currentTabIndex === 0;
  const isLastTab = currentTabIndex === tabs.length - 1;

  const goToPreviousTab = () => {
    if (!isFirstTab) {
      setCurrentTab(tabs[currentTabIndex - 1].key);
    }
  };

  const goToNextTab = () => {
    if (!isLastTab) {
      setCurrentTab(tabs[currentTabIndex + 1].key);
    }
  };

  const listUrl = `/gjennomforinger/${gjennomforingId}/tilskudd-behandling`;

  const onSubmit = handleSubmit((data) => {
    mutation.mutate(data, {
      onSuccess: () => navigate(listUrl),
      onValidationError: (error: ValidationError) => {
        error.errors.forEach((fieldError) => {
          const name = jsonPointerToFieldPath(
            fieldError.pointer,
          ) as keyof TilskuddBehandlingRequest;
          setError(name, { type: "custom", message: fieldError.detail });
        });
      },
    });
  });

  function tabHasErrors(tab: Tab): boolean {
    const vedtakFields = ["vedtakResultat", "kommentarVedtaksbrev"];
    const allVedtakErrors = Array.isArray(errors.vedtak)
      ? errors.vedtak.flatMap((v) => Object.keys(v ?? {}))
      : [];

    switch (tab.key) {
      case "vedtak":
        return Array.isArray(errors.vedtak)
          ? errors.vedtak.some((v) => vedtakFields.some((field) => field in (v ?? {})))
          : false;
      case "saksopplysninger":
        return (
          Object.keys(errors).some((field) => field !== "vedtak") ||
          allVedtakErrors.some((field) => !vedtakFields.includes(field))
        );
    }
  }

  return (
    <FormProvider {...form}>
      <form onSubmit={onSubmit}>
        <TilskuddBehandlingLayout
          opprettelse={behandling?.opprettelse}
          gjennomforingId={gjennomforingId}
          currentTab={currentTab}
          onTabChange={setCurrentTab}
          tabList={tabs.map((tab) => (
            <TabWithErrorBorder
              key={tab.key}
              onClick={() => {}}
              value={tab.key}
              label={tab.label}
              hasError={tabHasErrors(tab)}
            />
          ))}
          saksopplysningerContent={<SaksopplysningerForm />}
          vedtakContent={<VedtakForm />}
          actions={
            <HStack gap="space-8" marginBlock="space-16" justify="end">
              {isFirstTab ? (
                <Button
                  variant="tertiary"
                  size="small"
                  type="button"
                  onClick={() => navigate(listUrl)}
                >
                  Avbryt
                </Button>
              ) : (
                <Button variant="tertiary" size="small" type="button" onClick={goToPreviousTab}>
                  Tilbake
                </Button>
              )}
              {isLastTab ? (
                <HStack gap="space-4" align="center">
                  <ValideringsfeilOppsummering />
                  <Button
                    variant="primary"
                    size="small"
                    type="submit"
                    disabled={mutation.isPending}
                  >
                    {mutation.isPending ? "Sender til attestering..." : "Send til attestering"}
                  </Button>
                </HStack>
              ) : (
                <Button variant="primary" size="small" type="button" onClick={goToNextTab}>
                  Neste
                </Button>
              )}
            </HStack>
          }
        />
      </form>
    </FormProvider>
  );
}
