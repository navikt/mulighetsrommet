import { useOpprettTilskuddBehandling } from "@/api/tilskudd-behandling/mutations";
import { defaultTilskuddRequest } from "@/components/tilskudd-behandling/defaultTilskuddRequest";
import { SaksopplysningerForm } from "@/components/tilskudd-behandling/SaksopplysningerForm";
import { VedtakForm } from "@/components/tilskudd-behandling/VedtakForm";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { TilskuddBehandlingRequest, ValidationError } from "@tiltaksadministrasjon/api-client";
import { Box, Button, HStack } from "@navikt/ds-react";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import { useNavigate, useParams } from "react-router";
import { v4 } from "uuid";
import { TilskuddBehandlingLayout } from "@/components/tilskudd-behandling/TilskuddBehandlingLayout";
import { useTilskuddBehandling } from "@/api/tilskudd-behandling/useTilskuddBehandling";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useEnkeltplassGjennomforingOrError } from "@/api/gjennomforing/useGjennomforing";
import { TotrinnskontrollReturnert } from "@/components/totrinnskontroll/TotrinnskontrollReturnert";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { FilePdfIcon } from "@navikt/aksel-icons";
import { VedtaksbrevPdfModal } from "@/components/tilskudd-behandling/VedtaksbrevPdfModal";
import { useVedtaksbrevPdfBlobPost } from "@/api/tilskudd-behandling/useVedtaksbrevPdfBlob";
import { PrisOgBetalingsbetingelser } from "@/components/prismodell/PrisOgBetalingsbetingelser";
import { WizardStep } from "@/hooks/useWizardForm";
import { WizardForm } from "@/components/skjema/WizardForm";
import { saksopplysningerSchema, vedtakSchema } from "@/components/tilskudd-behandling/validation";

export function TilskuddBehandlingFormPage() {
  const { behandlingId } = useParams();
  return behandlingId ? (
    <RedigerTilskuddBehandling behandlingId={behandlingId} />
  ) : (
    <TilskuddBehandlingForm data={undefined} />
  );
}

function RedigerTilskuddBehandling({ behandlingId }: { behandlingId: string }) {
  const { data } = useTilskuddBehandling(behandlingId);
  return <TilskuddBehandlingForm data={data} />;
}

function TilskuddBehandlingForm({
  data,
}: {
  data: ReturnType<typeof useTilskuddBehandling>["data"] | undefined;
}) {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, prismodell } = useEnkeltplassGjennomforingOrError(gjennomforingId);
  const behandling = data?.behandling;
  const navigate = useNavigate();
  const mutation = useOpprettTilskuddBehandling(gjennomforingId);

  const defaultValues: TilskuddBehandlingRequest = behandling
    ? {
        id: behandling.id,
        gjennomforingId: behandling.gjennomforingId,
        tilskudd: behandling.tilskudd.map((t) => ({
          id: t.id,
          tilskuddId: t.tilskuddId,
          tilskuddOpplaeringType: t.tilskuddOpplaeringType,
          soknadBelop: t.soknadBelop,
          periodeStart: yyyyMMddFormatting(t.periode.start) ?? null,
          periodeSlutt: yyyyMMddFormatting(subDuration(t.periode.slutt, { days: 1 })) ?? null,
          vedtakResultat: t.vedtakResultat.type,
          kommentarVedtaksbrev: t.kommentarVedtaksbrev,
          utbetalingMottaker: t.utbetalingMottaker,
          belop: t.utbetalingBelop?.belop ?? null,
          kidNummer: t.kid,
          brukerUtbetalingType: t.brukerUtbetalingType,
          soknadJournalpostId: t.soknadJournalpostId,
          kostnadssted: t.kostnadssted.enhetsnummer,
          soknadDato: t.soknadDato,
          kommentarIntern: t.kommentarIntern,
        })),
      }
    : {
        id: v4(),
        gjennomforingId,
        tilskudd: [defaultTilskuddRequest(gjennomforing.ansvarligEnhet.enhetsnummer)],
      };

  const listUrl = `/gjennomforinger/${gjennomforingId}/tilskudd-behandling`;

  const retunert = data?.opprettelse && (
    <Box marginBlock="space-0 space-16">
      <TotrinnskontrollReturnert
        heading="Behandlingen ble returnert"
        opprettelse={data.opprettelse}
      />
    </Box>
  );
  const saksopplysningerSteg: WizardStep = {
    key: "Saksopplysninger",
    schema: saksopplysningerSchema,
    Component: (
      <>
        {retunert}
        <TwoColumnGrid separator>
          <Box>
            <SaksopplysningerForm
              arrangorId={gjennomforing.arrangor.id}
              ansvarligEnhet={gjennomforing.ansvarligEnhet}
            />
          </Box>
          <Box>
            <PrisOgBetalingsbetingelser prismodell={prismodell} size="medium" />
          </Box>
        </TwoColumnGrid>
      </>
    ),
  };
  const vedtakOgBeregningSteg: WizardStep = {
    key: "Vedtak og beregning",
    schema: vedtakSchema,
    Component: (
      <>
        {retunert}
        <TwoColumnGrid separator>
          <Box>
            <VedtakForm arrangorId={gjennomforing.arrangor.id} />
          </Box>
          <Box>
            <HStack align="end">
              <VedtaksbrevKnappOgModal />
            </HStack>
            <PrisOgBetalingsbetingelser prismodell={prismodell} size="medium" />
          </Box>
        </TwoColumnGrid>
      </>
    ),
  };

  const wizardLabels = {
    submit: "Send til godkjenning",
    next: `Neste: ${vedtakOgBeregningSteg.key}`,
  };

  return (
    <TilskuddBehandlingLayout gjennomforingId={gjennomforingId}>
      <WizardForm<TilskuddBehandlingRequest>
        steps={[saksopplysningerSteg, vedtakOgBeregningSteg]}
        defaultValues={defaultValues}
        onCancel={() => navigate(listUrl)}
        onSubmit={(data, form) => {
          mutation.mutate(data, {
            onSuccess: () => navigate(listUrl),
            onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
          });
        }}
        isSubmitting={mutation.isPending}
        labels={wizardLabels}
      />
    </TilskuddBehandlingLayout>
  );
}

function VedtaksbrevKnappOgModal() {
  const vedtaksbrevMutation = useVedtaksbrevPdfBlobPost();
  const [pdfPreviewOpen, setPdfPreviewOpen] = useState(false);
  const form = useFormContext<TilskuddBehandlingRequest>();
  function onVisVedtaksbrev() {
    const data = form.getValues();
    vedtaksbrevMutation.mutate(data, {
      onSuccess: () => setPdfPreviewOpen(true),
      onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
    });
  }
  return (
    <>
      <Button
        className="ml-auto"
        variant="tertiary"
        size="small"
        type="button"
        onClick={onVisVedtaksbrev}
        disabled={vedtaksbrevMutation.isPending}
        icon={<FilePdfIcon aria-hidden />}
      >
        Vis vedtaksbrev
      </Button>
      <VedtaksbrevPdfModal
        blob={vedtaksbrevMutation.data}
        isLoading={vedtaksbrevMutation.isPending}
        isError={vedtaksbrevMutation.isError}
        open={pdfPreviewOpen}
        onClose={() => setPdfPreviewOpen(false)}
      />
    </>
  );
}
