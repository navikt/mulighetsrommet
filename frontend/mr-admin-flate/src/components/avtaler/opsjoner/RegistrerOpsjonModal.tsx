import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, BodyLong, BodyShort, Button, Modal, VStack } from "@navikt/ds-react";
import { AvtaleDto, OpsjonLoggRequest, OpsjonStatus } from "@mr/api-client";
import { RefObject } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useRegistrerOpsjon } from "@/api/avtaler/useRegistrerOpsjon";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { OpsjonerRegistrert } from "./OpsjonerRegistrert";
import {
  InferredRegistrerOpsjonSchema,
  Opsjonsvalg,
  RegistrerOpsjonSchema,
} from "./RegistrerOpsjonSchema";
import { RegistrerOpsjonSkjema } from "./RegistrerOpsjonSkjema";
import { useRevalidator } from "react-router-dom";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  avtale: AvtaleDto;
}

export function RegistrerOpsjonModal({ modalRef, avtale }: Props) {
  const mutation = useRegistrerOpsjon();
  const revalidator = useRevalidator();
  const form = useForm<InferredRegistrerOpsjonSchema>({
    resolver: zodResolver(RegistrerOpsjonSchema),
    defaultValues: {},
  });

  const { handleSubmit, reset } = form;

  const postData: SubmitHandler<InferredRegistrerOpsjonSchema> = async (data): Promise<void> => {
    const request: OpsjonLoggRequest = {
      avtaleId: avtale.id,
      nySluttdato: data.opsjonsdatoValgt || null,
      forrigeSluttdato: avtale?.sluttDato || null,
      status: getStatus(data.opsjonsvalg),
    };

    mutation.mutate(request, {
      onSuccess: () => {
        closeAndResetForm();
      },
    });
  };

  function getStatus(opsjonsvalg: Opsjonsvalg): OpsjonStatus {
    switch (opsjonsvalg) {
      case "1":
        return OpsjonStatus.OPSJON_UTLØST;
      case "Annet":
        return OpsjonStatus.OPSJON_UTLØST;
      case "Opsjon_skal_ikke_utloses":
        return OpsjonStatus.SKAL_IKKE_UTLØSE_OPSJON;
    }
  }

  function closeAndResetForm() {
    reset();
    mutation.reset();
    modalRef?.current?.close();
    revalidator.revalidate();
  }

  function sluttDatoErLikEllerPassererMaksVarighet(): boolean {
    if (avtale?.opsjonsmodellData?.opsjonMaksVarighet && avtale?.sluttDato) {
      return new Date(avtale?.sluttDato) >= new Date(avtale?.opsjonsmodellData?.opsjonMaksVarighet);
    }
    return false;
  }

  if (sluttDatoErLikEllerPassererMaksVarighet()) {
    return <SluttDatoErLikEllerPassererMaksVarighetModal modalRef={modalRef} avtale={avtale} />;
  }

  const avtaleSkalIkkeUtloseOpsjoner = avtale?.opsjonerRegistrert?.some(
    (opsjon) => opsjon.status === OpsjonStatus.SKAL_IKKE_UTLØSE_OPSJON,
  );

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
              <BodyLong as="div">
                {!avtaleSkalIkkeUtloseOpsjoner && <RegistrerOpsjonSkjema avtale={avtale} />}

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
  avtale,
}: {
  modalRef: RefObject<HTMLDialogElement>;
  avtale: AvtaleDto;
}) {
  return (
    <VarselModal
      headingIconType="info"
      headingText="Kan ikke registrere opsjon"
      handleClose={() => modalRef?.current?.close()}
      modalRef={modalRef}
      primaryButton={<Button onClick={() => modalRef?.current?.close()}>Ok</Button>}
      body={
        <VStack gap="5">
          <BodyShort>
            Du kan ikke registrere flere opsjoner for avtalen. Avtalens sluttdato er samme som maks
            varighet for avtalen.
          </BodyShort>
          <OpsjonerRegistrert readOnly={false} avtale={avtale} />
        </VStack>
      }
    />
  );
}
