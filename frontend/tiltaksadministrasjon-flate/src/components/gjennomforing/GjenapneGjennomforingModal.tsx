import { useGjenapneGjennomforing } from "@/api/gjennomforing/useGjenapneGjennomforing";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { ValideringsfeilOppsummering } from "@/components/skjema/ValideringsfeilOppsummering";
import { BodyShort, Button, InfoCard, Modal, VStack } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { addDuration } from "@mr/frontend-common/utils/date";

interface Props {
  open: boolean;
  setOpen(open: boolean): void;
  gjennomforingId: string;
}

interface GjenapneFormValues {
  nySluttDato: string;
}

const formId = "gjenapne-gjennomforing-form";

export function GjenapneGjennomforingModal({ open, setOpen, gjennomforingId }: Props) {
  const mutation = useGjenapneGjennomforing();

  const form = useForm<GjenapneFormValues>({
    defaultValues: { nySluttDato: "" },
  });

  function closeAndReset() {
    form.reset();
    mutation.reset();
    setOpen(false);
  }

  const onSubmit: SubmitHandler<GjenapneFormValues> = (data) => {
    mutation.mutate(
      { id: gjennomforingId, nySluttDato: data.nySluttDato },
      {
        onSuccess: closeAndReset,
        onValidationError: (validation: ValidationError) => applyValidationErrors(form, validation),
      },
    );
  };

  return (
    <Modal
      open={open}
      onClose={closeAndReset}
      header={{ heading: "Gjenåpne gjennomføring" }}
      width="medium"
    >
      <FormProvider {...form}>
        <Modal.Body>
          <VStack gap="space-16">
            <InfoCard data-color="warning">
              <InfoCard.Header>
                <InfoCard.Title>Viktig informasjon</InfoCard.Title>
              </InfoCard.Header>
              <InfoCard.Content>
                <BodyShort spacing>
                  Gjennomføringen vil gjenåpnes til den nye sluttdatoen du oppgir.
                </BodyShort>
                <BodyShort>
                  Siden gjennomføringen har vært avsluttet så er alle tidligere deltakere også
                  avsluttet. Deltakere må søkes inn på nytt om de fortsatt skal delta på tiltaket.
                </BodyShort>
              </InfoCard.Content>
            </InfoCard>
            <form id={formId} onSubmit={form.handleSubmit(onSubmit)}>
              <FormDateInput<GjenapneFormValues>
                name="nySluttDato"
                label="Ny sluttdato"
                fromDate={new Date()}
                toDate={addDuration(new Date(), { years: 6 })}
                invalidDatoForTidlig="Ny sluttdato må være i dag eller i fremtiden"
                rules={{
                  required: "Ny sluttdato er påkrevd",
                }}
              />
            </form>
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          <Button size="small" form={formId} disabled={mutation.isPending}>
            {mutation.isPending ? "Lagrer..." : "Gjenåpne gjennomføring"}
          </Button>
          <Button size="small" type="button" variant="tertiary" onClick={closeAndReset}>
            Avbryt
          </Button>
          <ValideringsfeilOppsummering />
        </Modal.Footer>
      </FormProvider>
    </Modal>
  );
}
