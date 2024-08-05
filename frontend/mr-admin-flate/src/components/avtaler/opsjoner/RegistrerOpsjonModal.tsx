import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, BodyLong, Button, HStack, Modal, VStack } from "@navikt/ds-react";
import { Avtale, OpsjonLoggRequest, OpsjonStatus } from "mulighetsrommet-api-client";
import { RefObject } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useRegistrerOpsjon } from "../../../api/avtaler/useRegistrerOpsjon";
import { VarselModal } from "../../modal/VarselModal";
import { InferredRegistrerOpsjonSchema, RegistrerOpsjonSchema } from "./RegistrerOpsjonSchema";
import { RegistrerOpsjonSkjema } from "./RegistrerOpsjonSkjema";
import { OpsjonerRegistrert } from "./OpsjonerRegistrert";

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

  const postData: SubmitHandler<InferredRegistrerOpsjonSchema> = async (data): Promise<void> => {
    const request: OpsjonLoggRequest = {
      avtaleId: avtale.id,
      nySluttdato: data.opsjonsdatoValgt || null,
      status: OpsjonStatus.OPSJON_UTLØST,
    };

    mutation.mutate(request, {
      onSuccess: () => {
        closeAndResetForm();
      },
    });
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
            <VStack gap="5">
              <BodyLong>
                <RegistrerOpsjonSkjema avtale={avtale} />
                {mutation.isError && (
                  <Alert variant="error">Noe gikk galt ved registrering av opsjon</Alert>
                )}
              </BodyLong>
              {avtale.opsjonerRegistrert.length > 0 ? (
                <OpsjonerRegistrert readOnly={false} avtale={avtale} />
              ) : null}
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
