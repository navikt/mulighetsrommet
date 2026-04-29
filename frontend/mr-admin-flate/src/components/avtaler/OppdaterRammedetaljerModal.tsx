import { RammedetaljerRequest, ValidationError } from "@tiltaksadministrasjon/api-client";
import { Button, HStack, Modal } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { useUpsertRammedetaljer } from "@/api/avtaler/useUpsertRammedetaljer";
import AvtaleRammedetaljerForm from "./AvtaleRammedetaljerForm";
import { useAvtaleRammedetaljerDefaults } from "@/api/avtaler/useAvtaleRammedetaljerDefaults";
import { useDeleteRammedetaljer } from "@/api/avtaler/useDeleteAvtaleRammedetaljer";
import { TrashFillIcon } from "@navikt/aksel-icons";

interface Props {
  avtaleId: string;
  onClose: () => void;
}

const formId = "oppdater-rammedetaljer-form";

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
      onSuccess: closeAndResetForm,
      onValidationError: (validation: ValidationError) => applyValidationErrors(form, validation),
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
        <Modal.Body>
          <form id={formId} onSubmit={form.handleSubmit(postData)}>
            <AvtaleRammedetaljerForm valuta={rammeDetaljerDefaults.valuta} />
          </form>
        </Modal.Body>
        <Modal.Footer>
          <HStack justify="space-between" className="flex-row-reverse" width="100%">
            <HStack gap="space-8" className="flex-row-reverse">
              <Button form={formId} size="small" disabled={mutation.isPending}>
                {mutation.isPending ? "Lagrer..." : "Bekreft"}
              </Button>
              <Button type="button" size="small" variant="tertiary" onClick={closeAndResetForm}>
                Avbryt
              </Button>
              <ValideringsfeilOppsummering />
            </HStack>
            {rammeDetaljerDefaults.totalRamme && rammeDetaljerDefaults.totalRamme > 0 && (
              <Button
                type="button"
                size="small"
                variant="secondary"
                data-color="neutral"
                disabled={deletion.isPending}
                onClick={deleteRammedetaljer}
                icon={<TrashFillIcon />}
              >
                {deletion.isPending ? "Sletter..." : "Slett"}
              </Button>
            )}
          </HStack>
        </Modal.Footer>
      </FormProvider>
    </Modal>
  );
}
