import {
  RammedetaljerDto,
  RammedetaljerRequest,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { Button, HStack, Modal } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { useUpsertRammedetaljer } from "@/api/avtaler/useUpsertRammedetaljer";
import AvtaleRammeDetaljerForm from "./AvtaleRammeDetaljerForm";
import { useAvtaleRammedetaljer } from "@/api/avtaler/useAvtaleRammedetaljer";

interface Props {
  avtaleId: string;
  onClose: () => void;
}

export function OppdaterRammedetaljerModal({ onClose, avtaleId }: Props) {
  const { data: rammeDetaljer } = useAvtaleRammedetaljer(avtaleId);
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
      width={450}
      size="small"
      closeOnBackdropClick
      onClose={closeAndResetForm}
      header={{ heading: "Oppdater rammedetaljer" }}
      open
    >
      <FormProvider {...form}>
        <form onSubmit={form.handleSubmit(postData)}>
          <Modal.Body>
            <AvtaleRammeDetaljerForm />
          </Modal.Body>
          <Modal.Footer>
            <HStack gap="1" className="flex-row-reverse">
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Lagrer..." : "Bekreft"}
              </Button>
              <Button type="button" variant="tertiary" onClick={closeAndResetForm}>
                Avbryt
              </Button>
              <ValideringsfeilOppsummering />
            </HStack>
          </Modal.Footer>
        </form>
      </FormProvider>
    </Modal>
  );
}

function defaultValues(rammeDetaljer: RammedetaljerDto | null): RammedetaljerRequest {
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
