import { PrismodellSchema, PrismodellValues } from "@/schemas/avtale";
import { zodResolver } from "@hookform/resolvers/zod";
import { AvtaleDto, PrismodellDto, ValidationError } from "@tiltaksadministrasjon/api-client";
import { Button, InfoCard, Modal, VStack } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import AvtalePrismodellForm from "./AvtalePrismodellForm";
import { useUpsertPrismodell } from "@/api/avtaler/useUpsertPrismodell";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { safeParseDate } from "@mr/frontend-common/utils/date";
import { toPrismodellRequest } from "@/pages/avtaler/avtaleFormUtils";

interface Props {
  open: boolean;
  avtale: AvtaleDto;
  onClose: () => void;
}

export function OppdaterPrisModal({ open, onClose, avtale }: Props) {
  const mutation = useUpsertPrismodell(avtale.id);
  const form = useForm<PrismodellValues>({
    resolver: zodResolver(PrismodellSchema),
    defaultValues: defaultValues(avtale.prismodeller),
  });

  const postData: SubmitHandler<PrismodellValues> = async (data): Promise<void> => {
    const request = toPrismodellRequest({ data });

    mutation.mutate(request, {
      onSuccess: () => {
        closeAndResetForm();
      },
      onValidationError: (validation: ValidationError) => {
        validation.errors.forEach((error) => {
          const name = jsonPointerToFieldPath(error.pointer);
          form.setError(name as keyof PrismodellValues, {
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
      open={open}
      header={{ heading: "Oppdater pris" }}
    >
      <FormProvider {...form}>
        <form onSubmit={form.handleSubmit(postData)}>
          <Modal.Body>
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
              <AvtalePrismodellForm
                tiltakskode={avtale.tiltakstype.tiltakskode}
                avtaleStartDato={safeParseDate(avtale.startDato)}
              />
            </VStack>
          </Modal.Body>
          <Modal.Footer>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Lagrer..." : "Bekreft"}
            </Button>
            <Button type="button" variant="tertiary" onClick={closeAndResetForm}>
              Avbryt
            </Button>
          </Modal.Footer>
        </form>
      </FormProvider>
    </Modal>
  );
}

function defaultValues(prismodeller: PrismodellDto[]): PrismodellValues {
  return {
    prismodeller: prismodeller.map((prismodell) => ({
      id: prismodell.id || undefined,
      type: prismodell.type,
      prisbetingelser: prismodell.prisbetingelser,
      satser: prismodell.satser ?? [],
    })),
  };
}
