import { RammedetaljerRequest, ValidationError } from "@tiltaksadministrasjon/api-client";
import { Button, HStack, Modal } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { useUpsertRammedetaljer } from "@/api/avtaler/useUpsertRammedetaljer";
import AvtaleRammeDetaljerForm from "./AvtaleRammeDetaljerForm";
import { useAvtaleRammedetaljerDefaults } from "@/api/avtaler/useAvtaleRammedetaljerDefaults";
import { useDeleteRammedetaljer } from "@/api/avtaler/useDeleteAvtaleRammedetaljer";
import { TrashFillIcon } from "@navikt/aksel-icons";

interface Props {
  avtaleId: string;
  onClose: () => void;
}

export function OppdaterRammedetaljerModal({ onClose, avtaleId }: Props) {
  const { data: rammeDetaljerDefaults } = useAvtaleRammedetaljerDefaults(avtaleId);
  const deletion = useDeleteRammedetaljer(avtaleId);
  const mutation = useUpsertRammedetaljer(avtaleId);
  const form = useForm<RammedetaljerRequest>({
    defaultValues: {
      totalRamme: rammeDetaljerDefaults.totalRamme,
      utbetaltArena: rammeDetaljerDefaults.utbetaltArena,
    },
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

  async function deleteRammedetaljer() {
    return deletion.mutate(undefined, {
      onSuccess: closeAndResetForm,
    });
  }

  function closeAndResetForm() {
    form.reset();
    mutation.reset();
    onClose();
  }

  return (
    <Modal
      width={500}
      closeOnBackdropClick
      onClose={closeAndResetForm}
      header={{ heading: "Oppdater rammedetaljer" }}
      open
    >
      <FormProvider {...form}>
        <form onSubmit={form.handleSubmit(postData)}>
          <Modal.Body>
            <AvtaleRammeDetaljerForm valuta={rammeDetaljerDefaults.valuta} />
          </Modal.Body>
          <Modal.Footer>
            <HStack justify="space-between" className="flex-row-reverse" width="100%">
              <HStack gap="2" className="flex-row-reverse">
                <Button type="submit" disabled={mutation.isPending}>
                  {mutation.isPending ? "Lagrer..." : "Bekreft"}
                </Button>
                <Button type="button" variant="tertiary" onClick={closeAndResetForm}>
                  Avbryt
                </Button>
                <ValideringsfeilOppsummering />
              </HStack>
              <Button
                type="button"
                variant="tertiary-neutral"
                disabled={deletion.isPending}
                onClick={deleteRammedetaljer}
                icon={<TrashFillIcon />}
              >
                {deletion.isPending ? "Sletter..." : "Slett"}
              </Button>
            </HStack>
          </Modal.Footer>
        </form>
      </FormProvider>
    </Modal>
  );
}
