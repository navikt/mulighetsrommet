import { OpprettUtbetalingRequest, UtbetalingDto } from "@tiltaksadministrasjon/api-client";
import { Button, HStack, Modal } from "@navikt/ds-react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { FormGroup } from "@/components/skjema/FormGroup";
import { ArrangorBetalingsinformasjon } from "@/pages/gjennomforing/utbetaling/ArrangorBetalingsinformasjon";
import { NumberInput } from "@/components/skjema/NumberInput";
import { TextareaInput } from "@/components/skjema/TextareaInput";
import { FormProvider } from "react-hook-form";
import { useOpprettUtbetalingForm } from "@/pages/gjennomforing/utbetaling/useOpprettUtbetalingForm";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";

interface OpprettKorreksjonModalProps {
  utbetaling: UtbetalingDto;
  open: boolean;
  close: () => void;
}

export function OpprettKorreksjonModal({ utbetaling, open, close }: OpprettKorreksjonModalProps) {
  const { gjennomforing, prismodell } = useGjennomforing(utbetaling.gjennomforingId);

  const { form, onSubmit } = useOpprettUtbetalingForm({
    gjennomforingId: gjennomforing.id,
    korrigererUtbetaling: utbetaling.id,
    periodeStart: utbetaling.periode.start,
    periodeSlutt: yyyyMMddFormatting(subDuration(utbetaling.periode.slutt, { days: 1 })),
    pris: { belop: null, valuta: prismodell.valuta },
  });

  return (
    <Modal onClose={close} open={open} header={{ heading: "Opprett korreksjon" }} width={1200}>
      <Modal.Body>
        <FormProvider {...form}>
          <form method="dialog" id="opprett-korreksjon" onSubmit={onSubmit}>
            <TwoColumnGrid separator>
              <FormGroup>
                <NumberInput<OpprettUtbetalingRequest>
                  label={`Beløp (${prismodell.valuta})`}
                  name="pris.belop"
                />
                <TextareaInput<OpprettUtbetalingRequest>
                  label="Begrunnelse for utbetaling"
                  name="korreksjonBegrunnelse"
                />
              </FormGroup>
              <FormGroup>
                {open && <ArrangorBetalingsinformasjon arrangorId={gjennomforing.arrangor.id} />}
              </FormGroup>
            </TwoColumnGrid>
          </form>
        </FormProvider>
      </Modal.Body>
      <Modal.Footer>
        <HStack gap="space-16">
          <Button type="button" variant="secondary" onClick={close}>
            Avbryt
          </Button>
          <Button form="opprett-korreksjon">Opprett korreksjon</Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
