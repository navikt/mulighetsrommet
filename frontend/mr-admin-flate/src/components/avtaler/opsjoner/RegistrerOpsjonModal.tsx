import { useRegistrerOpsjon } from "@/api/avtaler/useRegistrerOpsjon";
import {
  AvtaleDto,
  OpsjonLoggStatus,
  OpprettOpsjonLoggRequest,
  OpprettOpsjonLoggRequestType,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { BodyLong, BodyShort, Button, Modal, VStack } from "@navikt/ds-react";
import { RefObject } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { RegistrerteOpsjoner } from "./RegistrerteOpsjoner";
import { RegistrerOpsjonForm } from "./RegistrerOpsjonForm";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  avtale: AvtaleDto;
}

export function RegistrerOpsjonModal({ modalRef, avtale }: Props) {
  const mutation = useRegistrerOpsjon(avtale.id);
  const form = useForm<OpprettOpsjonLoggRequest>({
    resolver: async (values) => ({ values, errors: {} }),
    defaultValues: {
      type: OpprettOpsjonLoggRequestType.ETT_AAR,
    },
  });
  const { setError } = form;

  const postData: SubmitHandler<OpprettOpsjonLoggRequest> = async (data): Promise<void> => {
    mutation.mutate(data, {
      onSuccess: () => {
        closeAndResetForm();
      },
      onValidationError: (error: ValidationError) => {
        error.errors.forEach((error: { pointer: string; detail: string }) => {
          const name = jsonPointerToFieldPath(error.pointer) as keyof OpprettOpsjonLoggRequest;
          setError(name, { type: "custom", message: error.detail });
        });
      },
    });
  };

  function closeAndResetForm() {
    form.reset();
    mutation.reset();
    modalRef.current?.close();
  }

  function sluttDatoErLikEllerPassererMaksVarighet(): boolean {
    if (avtale.opsjonsmodell.opsjonMaksVarighet && avtale.sluttDato) {
      return new Date(avtale.sluttDato) >= new Date(avtale.opsjonsmodell.opsjonMaksVarighet);
    }
    return false;
  }

  if (sluttDatoErLikEllerPassererMaksVarighet()) {
    return <SluttDatoErLikEllerPassererMaksVarighetModal modalRef={modalRef} avtale={avtale} />;
  }

  const avtaleSkalIkkeUtloseOpsjoner = avtale.opsjonerRegistrert.some(
    (opsjon) => opsjon.status === OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON,
  );

  return (
    <Modal
      width={1000}
      closeOnBackdropClick
      onClose={closeAndResetForm}
      ref={modalRef}
      header={{ heading: "Registrer opsjon" }}
    >
      <FormProvider {...form}>
        <form onSubmit={form.handleSubmit(postData)}>
          <Modal.Body>
            <VStack gap="5">
              <BodyLong as="div">
                {!avtaleSkalIkkeUtloseOpsjoner && <RegistrerOpsjonForm avtale={avtale} />}
              </BodyLong>
              {avtale.opsjonerRegistrert.length > 0 ? (
                <RegistrerteOpsjoner readOnly={false} />
              ) : null}
            </VStack>
          </Modal.Body>
          <Modal.Footer>
            {!avtaleSkalIkkeUtloseOpsjoner && (
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Lagrer..." : "Bekreft"}
              </Button>
            )}
            <Button type="button" variant="tertiary" onClick={closeAndResetForm}>
              Avbryt
            </Button>
          </Modal.Footer>
        </form>
      </FormProvider>
    </Modal>
  );
}

interface ModalProps {
  modalRef: RefObject<HTMLDialogElement | null>;
  avtale: AvtaleDto;
}

function SluttDatoErLikEllerPassererMaksVarighetModal({ modalRef }: ModalProps) {
  return (
    <VarselModal
      headingIconType="info"
      headingText="Kan ikke registrere opsjon"
      handleClose={() => modalRef.current?.close()}
      modalRef={modalRef}
      primaryButton={<Button onClick={() => modalRef.current?.close()}>Ok</Button>}
      body={
        <VStack gap="5">
          <BodyShort>
            Du kan ikke registrere flere opsjoner for avtalen. Avtalens sluttdato er samme som maks
            varighet for avtalen.
          </BodyShort>
          <RegistrerteOpsjoner readOnly={false} />
        </VStack>
      }
    />
  );
}
