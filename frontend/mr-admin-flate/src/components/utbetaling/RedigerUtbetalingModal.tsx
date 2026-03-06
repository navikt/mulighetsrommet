import { Link as ReactRouterLink } from "react-router";
import { UtbetalingDto } from "@tiltaksadministrasjon/api-client";
import { Button, HStack, InfoCard, Link, Modal, VStack } from "@navikt/ds-react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { MetadataHGrid } from "@mr/frontend-common/components/datadriven/Metadata";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { useRedigerUtbetalingForm } from "@/components/utbetaling/form/useRedigerUtbetalingForm";
import { FormProvider } from "react-hook-form";
import { UtbetalingForm } from "@/components/utbetaling/form/UtbetalingForm";

interface RedigerUtbetalingModalProps {
  utbetaling: UtbetalingDto;
  open: boolean;
  close: () => void;
}

export function RedigerUtbetalingModal({ utbetaling, open, close }: RedigerUtbetalingModalProps) {
  const { gjennomforing } = useGjennomforing(utbetaling.gjennomforingId);
  const { form, submit } = useRedigerUtbetalingForm(utbetaling, { onSuccess: close });

  const formId = "rediger-utbetaling";
  return (
    <Modal onClose={close} open={open} header={{ heading: "Rediger utbetaling" }} width={1200}>
      <Modal.Body>
        <VStack gap="space-24">
          {utbetaling.korreksjon && <KorreksjonInfoCard utbetaling={utbetaling} />}
          <FormProvider {...form}>
            <UtbetalingForm
              id={formId}
              onSubmit={form.handleSubmit(submit)}
              arrangorId={gjennomforing.arrangor.id}
              startDato={gjennomforing.startDato}
            />
          </FormProvider>
        </VStack>
      </Modal.Body>
      <Modal.Footer>
        <HStack gap="space-16">
          <Button size="small" variant="tertiary" onClick={close}>
            Avbryt
          </Button>
          <Button size="small" form={formId}>
            Lagre
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}

function KorreksjonInfoCard({ utbetaling }: { utbetaling: UtbetalingDto }) {
  return (
    <InfoCard>
      <InfoCard.Header>
        <InfoCard.Title>Dette er en korreksjon for følgende utbetaling</InfoCard.Title>
      </InfoCard.Header>
      <InfoCard.Content>
        <MetadataHGrid
          label={utbetalingTekster.metadata.periode}
          value={formaterPeriode(utbetaling.periode)}
        />
        {utbetaling.korreksjon?.opprinneligUtbetaling && (
          <MetadataHGrid
            label={utbetalingTekster.korreksjon.gjelderUtbetaling}
            value={
              <Link
                as={ReactRouterLink}
                to={`/gjennomforinger/${utbetaling.gjennomforingId}/utbetalinger/${utbetaling.korreksjon.opprinneligUtbetaling}`}
              >
                Opprinnelig utbetaling
              </Link>
            }
          />
        )}
      </InfoCard.Content>
    </InfoCard>
  );
}
