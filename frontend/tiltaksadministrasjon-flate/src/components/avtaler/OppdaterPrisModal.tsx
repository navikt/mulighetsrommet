import { PrismodellSchema, PrismodellValues } from "@/pages/avtaler/form/validation";
import { zodResolver } from "@hookform/resolvers/zod";
import { AvtaleDto, PrismodellDto, ValidationError } from "@tiltaksadministrasjon/api-client";
import { Button, InfoCard, Modal, VStack } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import AvtalePrismodellForm from "./AvtalePrismodellForm";
import { useUpsertPrismodell } from "@/api/avtaler/useUpsertPrismodell";
import { safeParseDate } from "@mr/frontend-common/utils/date";
import { toPrismodellRequest } from "@/pages/avtaler/form/mappers";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { applyValidationErrors } from "@/components/skjema/helpers";

interface Props {
  open: boolean;
  avtale: AvtaleDto;
  onClose: () => void;
}

const formId = "oppdater-pris-form";

export function OppdaterPrisModal({ open, onClose, avtale }: Props) {
  const mutation = useUpsertPrismodell(avtale.id);

  const form = useForm<PrismodellValues>({
    resolver: zodResolver(PrismodellSchema),
    defaultValues: defaultValues(avtale.prismodeller),
  });

  const postData: SubmitHandler<PrismodellValues> = async (data): Promise<void> => {
    const request = toPrismodellRequest(data);

    mutation.mutate(request, {
      onSuccess: closeAndResetForm,
      onValidationError: (validation: ValidationError) => applyValidationErrors(form, validation),
    });
  };

  function closeAndResetForm() {
    form.reset();
    mutation.reset();
    onClose();
  }

  return (
    <Modal
      width={900}
      closeOnBackdropClick
      onClose={closeAndResetForm}
      open={open}
      header={{ heading: "Oppdater pris" }}
    >
      <FormProvider {...form}>
        <Modal.Body className="max-h-[70vh] overflow-y-auto">
          <form id={formId} onSubmit={form.handleSubmit(postData)}>
            <VStack gap="space-16">
              <InfoCard data-color="warning">
                <InfoCard.Header>
                  <InfoCard.Title>Endring av prismodell</InfoCard.Title>
                </InfoCard.Header>
                <InfoCard.Content>
                  Vær oppmerksom på at hvis du gjør endringer i en prismodell som er i bruk, kan det
                  påvirke beregningen i krav arrangøren ikke har sendt inn ennå.
                </InfoCard.Content>
              </InfoCard>
              <AvtalePrismodellForm
                tiltakskode={avtale.tiltakstype.tiltakskode}
                avtaleStartDato={safeParseDate(avtale.startDato)}
              />
            </VStack>
          </form>
        </Modal.Body>
        <Modal.Footer>
          <Button form={formId} disabled={mutation.isPending}>
            {mutation.isPending ? "Lagrer..." : "Bekreft"}
          </Button>
          <Button type="button" variant="tertiary" onClick={closeAndResetForm}>
            Avbryt
          </Button>
          <ValideringsfeilOppsummering />
        </Modal.Footer>
      </FormProvider>
    </Modal>
  );
}

function defaultValues(prismodeller: PrismodellDto[]): PrismodellValues {
  return {
    prismodeller: prismodeller.map((prismodell) => ({
      id: prismodell.id || undefined,
      type: prismodell.type,
      valuta: prismodell.valuta,
      prisbetingelser: prismodell.prisbetingelser,
      satser:
        prismodell.satser?.map((sats) => ({
          gjelderFra: sats.gjelderFra,
          gjelderTil: sats.gjelderTil,
          pris: sats.pris.belop,
        })) ?? [],
      tilsagnPerDeltaker: prismodell.tilsagnPerDeltaker,
    })),
  };
}
