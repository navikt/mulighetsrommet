import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, BodyLong, Button, Modal } from "@navikt/ds-react";
import { Avtale, OpsjonLoggRequest, OpsjonStatus } from "mulighetsrommet-api-client";
import { RefObject, useEffect } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useRegistrerOpsjon } from "../../../api/avtaler/useRegistrerOpsjon";
import { VarselModal } from "../../modal/VarselModal";
import { InferredRegistrerOpsjonSchema, RegistrerOpsjonSchema } from "./RegistrerOpsjonSchema";
import { RegistrerOpsjonSkjema } from "./RegistrerOpsjonSkjema";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  avtale: Avtale;
}

export function RegistrerOpsjonModal({ modalRef, avtale }: Props) {
  const mutation = useRegistrerOpsjon();
  const form = useForm<InferredRegistrerOpsjonSchema>({
    resolver: zodResolver(RegistrerOpsjonSchema),
    defaultValues: {},
  });

  const { handleSubmit, reset } = form;

  useEffect(() => {
    if (mutation.isSuccess) {
      closeAndResetForm();
    }
  }, [mutation]);

  const postData: SubmitHandler<InferredRegistrerOpsjonSchema> = async (data): Promise<void> => {
    const request: OpsjonLoggRequest = {
      avtaleId: avtale.id,
      nySluttdato: data.opsjonsdatoValgt || null,
      status: OpsjonStatus.OPSJON_UTLØST,
    };

    await mutation.mutate(request);
  };

  function closeAndResetForm() {
    reset();
    mutation.reset();
    modalRef?.current?.close();
  }

  function sluttDatoErLikEllerPassererMaksVarighet(): boolean {
    if (avtale?.opsjonsmodellData?.opsjonMaksVarighet && avtale?.sluttDato) {
      return new Date(avtale?.sluttDato) >= new Date(avtale?.opsjonsmodellData?.opsjonMaksVarighet);
    }
    return false;
  }

  if (sluttDatoErLikEllerPassererMaksVarighet()) {
    return <SluttDatoErLikEllerPassererMaksVarighetModal modalRef={modalRef} />;
  }

  return (
    <Modal
      width={500}
      closeOnBackdropClick
      onClose={closeAndResetForm}
      ref={modalRef}
      header={{ heading: "Registrer opsjon" }}
    >
      <FormProvider {...form}>
        <form onSubmit={handleSubmit(postData)}>
          <Modal.Body>
            <BodyLong>
              <RegistrerOpsjonSkjema avtale={avtale} />
              {mutation.isError && (
                <Alert variant="error">Noe gikk galt ved registrering av opsjon</Alert>
              )}
            </BodyLong>
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

function SluttDatoErLikEllerPassererMaksVarighetModal({
  modalRef,
}: {
  modalRef: RefObject<HTMLDialogElement>;
}) {
  return (
    <VarselModal
      headingIconType="info"
      headingText="Kan ikke registrere opsjon"
      handleClose={() => modalRef?.current?.close()}
      modalRef={modalRef}
      primaryButton={<Button onClick={() => modalRef?.current?.close()}>Ok</Button>}
      body="Du kan ikke registrere flere opsjoner for avtalen. Avtalens sluttdato er samme som maks varighet for
        avtalen."
    />
  );
}
