import { UtbetalingDto } from "@tiltaksadministrasjon/api-client";
import { Button, HStack, Modal, VStack } from "@navikt/ds-react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useOpprettUtbetalingForm } from "@/components/utbetaling/form/useOpprettUtbetalingForm";
import { FormProvider } from "react-hook-form";
import { UtbetalingForm } from "@/components/utbetaling/form/UtbetalingForm";
import { KorreksjonInfoCard } from "@/components/utbetaling/KorreksjonInfoCard";

interface OpprettKorreksjonModalProps {
  utbetaling: UtbetalingDto;
  open: boolean;
  close: () => void;
}

export function OpprettKorreksjonModal({ utbetaling, open, close }: OpprettKorreksjonModalProps) {
  const { gjennomforing } = useGjennomforing(utbetaling.gjennomforingId);

  const { form, submit } = useOpprettUtbetalingForm({
    gjennomforingId: gjennomforing.id,
    korrigererUtbetaling: utbetaling.id,
    periodeStart: utbetaling.periode.start,
    periodeSlutt: yyyyMMddFormatting(subDuration(utbetaling.periode.slutt, { days: 1 })),
    pris: { belop: null, valuta: utbetaling.beregning.valuta },
  });

  const formId = "opprett-korreksjon";
  return (
    <Modal onClose={close} open={open} header={{ heading: "Opprett korreksjon" }} width={1200}>
      <Modal.Body>
        <VStack gap="space-24">
          <KorreksjonInfoCard utbetalingId={utbetaling.id} />
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
            Opprett
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
