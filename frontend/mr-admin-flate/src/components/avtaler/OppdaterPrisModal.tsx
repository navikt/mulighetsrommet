import { PrismodellSchema, PrismodellValues } from "@/schemas/avtale";
import { zodResolver } from "@hookform/resolvers/zod";
import { AvtaleDto, Prismodell, PrismodellDto, ValidationError } from "@mr/api-client-v2";
import { Button, Modal } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import AvtalePrismodellForm from "./AvtalePrismodellForm";
import { useUpsertPrismodell } from "@/api/avtaler/useUpsertPrismodell";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { parseDate } from "@mr/frontend-common/utils/date";

interface Props {
  open: boolean;
  avtale: AvtaleDto;
  onClose: () => void;
}

export function OppdaterPrisModal({ open, onClose, avtale }: Props) {
  const mutation = useUpsertPrismodell(avtale.id);
  const form = useForm<PrismodellValues>({
    resolver: zodResolver(PrismodellSchema),
    defaultValues: defaultValues(avtale.prismodell),
  });

  const postData: SubmitHandler<PrismodellValues> = async (data): Promise<void> => {
    const request = {
      ...data,
    };

    mutation.mutate(
      {
        type: request.prismodell,
        ...request,
      },
      {
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
      },
    );
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
            <AvtalePrismodellForm
              tiltakskode={avtale.tiltakstype.tiltakskode}
              avtaleStartDato={parseDate(avtale.startDato)!}
            />
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

function defaultValues(prismodell: PrismodellDto): PrismodellValues {
  switch (prismodell.type) {
    case "ANNEN_AVTALT_PRIS":
      return {
        prismodell: Prismodell.ANNEN_AVTALT_PRIS,
        prisbetingelser: prismodell.prisbetingelser ?? undefined,
        satser: [],
      };
    case "AVTALT_PRIS_PER_MANEDSVERK":
      return {
        ...prismodell,
        prismodell: Prismodell.AVTALT_PRIS_PER_MANEDSVERK,
        prisbetingelser: prismodell.prisbetingelser ?? undefined,
      };
    case "AVTALT_PRIS_PER_UKESVERK":
      return {
        ...prismodell,
        prismodell: Prismodell.AVTALT_PRIS_PER_UKESVERK,
        prisbetingelser: prismodell.prisbetingelser ?? undefined,
      };
    case "AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER":
      return {
        ...prismodell,
        prismodell: Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
        prisbetingelser: prismodell.prisbetingelser ?? undefined,
      };
    case "FORHANDSGODKJENT_PRIS_PER_MANEDSVERK":
      return {
        ...prismodell,
        prismodell: Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        satser: [],
      };
  }
}
