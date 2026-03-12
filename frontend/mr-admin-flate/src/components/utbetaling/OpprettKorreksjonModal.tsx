import { OpprettUtbetalingRequest, UtbetalingDto } from "@tiltaksadministrasjon/api-client";
import { Button, HStack, InfoCard, Modal, VStack } from "@navikt/ds-react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorBetalingsinformasjon } from "@/components/utbetaling/ArrangorBetalingsinformasjon";
import { useOpprettUtbetalingForm } from "@/components/utbetaling/form/useOpprettUtbetalingForm";
import { NumberInput } from "@/components/skjema/NumberInput";
import { FormProvider } from "react-hook-form";
import { formaterPeriode, subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { FormGroup } from "@/layouts/FormGroup";
import { MetadataHGrid } from "@mr/frontend-common/components/datadriven/Metadata";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";

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
        <VStack gap="space-24">
          <InfoCard>
            <InfoCard.Header>
              <InfoCard.Title>
                Du er i ferd med å opprette en korreksjon på en eksisterende utbetaling
              </InfoCard.Title>
            </InfoCard.Header>
            <InfoCard.Content>
              <MetadataHGrid
                label={utbetalingTekster.metadata.periode}
                value={formaterPeriode(utbetaling.periode)}
              />
              <MetadataHGrid
                label={utbetalingTekster.beregning.belop.label}
                value={formaterValutaBelop(utbetaling.pris)}
              />
            </InfoCard.Content>
          </InfoCard>
          <FormProvider {...form}>
            <form method="dialog" id="opprett-korreksjon" onSubmit={onSubmit}>
              <TwoColumnGrid separator>
                <FormGroup>
                  <NumberInput<OpprettUtbetalingRequest>
                    label={`Beløp (${prismodell.valuta})`}
                    name="pris.belop"
                  />
                  <FormTextarea<OpprettUtbetalingRequest>
                    label="Begrunnelse for utbetaling"
                    name="korreksjonBegrunnelse"
                    maxLength={250}
                  />
                </FormGroup>
                <FormGroup>
                  {open && <ArrangorBetalingsinformasjon arrangorId={gjennomforing.arrangor.id} />}
                </FormGroup>
              </TwoColumnGrid>
            </form>
          </FormProvider>
        </VStack>
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
