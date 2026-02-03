import {
  RammedetaljerDto,
  RammedetaljerRequest,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { Button, HStack, InfoCard, Modal, VStack } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { useUpsertRammedetaljer } from "@/api/avtaler/useUpsertRammedetaljer";
import AvtaleRammeDetaljerForm from "./AvtaleRammeDetaljerForm";

interface Props {
  avtaleId: string;
  rammeDetaljer?: RammedetaljerDto;
  onClose: () => void;
}

export function OppdaterRammedetaljerModal({ onClose, avtaleId, rammeDetaljer }: Props) {
  const mutation = useUpsertRammedetaljer(avtaleId);
  const form = useForm<RammedetaljerRequest>({
    defaultValues: defaultValues(rammeDetaljer),
  });

  const postData: SubmitHandler<RammedetaljerRequest> = async (
    request: RammedetaljerRequest,
  ): Promise<void> => {
    mutation.mutate(request, {
      onSuccess: () => {
        closeAndResetForm();
      },
      onValidationError: (validation: ValidationError) => {
        validation.errors.forEach((error) => {
          const name = jsonPointerToFieldPath(error.pointer);
          form.setError(name as keyof RammedetaljerRequest, {
            type: "custom",
            message: error.detail,
          });
        });
      },
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
      open={true}
      header={{ heading: "Oppdater rammedetaljer" }}
    >
      <FormProvider {...form}>
        <form onSubmit={form.handleSubmit(postData)}>
          <Modal.Body className="max-h-[70vh] overflow-y-auto">
            <VStack gap="4">
              <InfoCard data-color="warning">
                <InfoCard.Header>
                  <InfoCard.Title>Endring av prismodell</InfoCard.Title>
                </InfoCard.Header>
                <InfoCard.Content>
                  Vær oppmerksom på at hvis du gjør endringer i en prismodell som er i bruk, kan det
                  påvirke beregningen i krav arrangøren ikke har sendt inn ennå.
                </InfoCard.Content>
              </InfoCard>
              <AvtaleRammeDetaljerForm />
            </VStack>
          </Modal.Body>
          <Modal.Footer>
            <HStack gap="1" className="flex-row-reverse">
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Lagrer..." : "Bekreft"}
              </Button>
              <Button type="button" variant="tertiary" onClick={closeAndResetForm}>
                Avbryt
              </Button>
              {form.formState.errors.root && <ValideringsfeilOppsummering />}
            </HStack>
          </Modal.Footer>
        </form>
      </FormProvider>
    </Modal>
  );
}

function defaultValues(rammeDetaljer?: RammedetaljerDto): RammedetaljerRequest {
  if (!rammeDetaljer) {
    return {
      totalRamme: 0,
      utbetaltArena: null,
    };
  }
  return {
    totalRamme: rammeDetaljer.totalRamme.belop,
    utbetaltArena: rammeDetaljer.utbetaltArena?.belop ?? null,
  };
}
