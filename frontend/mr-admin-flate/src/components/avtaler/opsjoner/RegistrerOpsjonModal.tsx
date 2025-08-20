import { useRegistrerOpsjon } from "@/api/avtaler/useRegistrerOpsjon";
import { zodResolver } from "@hookform/resolvers/zod";
import { AvtaleDto, OpprettOpsjonLoggRequest, OpsjonStatus } from "@mr/api-client-v2";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { addDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { Alert, BodyLong, BodyShort, Button, Modal, VStack } from "@navikt/ds-react";
import { RefObject } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { RegistrerteOpsjoner } from "./RegistrerteOpsjoner";
import { RegistrerOpsjonForm } from "./RegistrerOpsjonForm";
import {
  InferredRegistrerOpsjonSchema,
  Opsjonsvalg,
  RegistrerOpsjonSchema,
} from "./RegistrerOpsjonSchema";

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  avtale: AvtaleDto;
}

export function RegistrerOpsjonModal({ modalRef, avtale }: Props) {
  const mutation = useRegistrerOpsjon(avtale.id);
  const form = useForm<InferredRegistrerOpsjonSchema>({
    resolver: zodResolver(RegistrerOpsjonSchema),
    defaultValues: { opsjonsvalg: "1" },
  });

  const postData: SubmitHandler<InferredRegistrerOpsjonSchema> = async (data): Promise<void> => {
    const request: OpprettOpsjonLoggRequest = {
      nySluttdato: getNesteSluttDato(data.opsjonsvalg, avtale.sluttDato, data.opsjonsdatoValgt),
      forrigeSluttdato: avtale.sluttDato || null,
      status: getStatus(data.opsjonsvalg),
    };

    mutation.mutate(request, {
      onSuccess: () => {
        closeAndResetForm();
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
    (opsjon) => opsjon.status === OpsjonStatus.SKAL_IKKE_UTLOSE_OPSJON,
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
        <form onSubmit={form.handleSubmit(postData)}>
          <Modal.Body>
            <VStack gap="5">
              <BodyLong as="div">
                {!avtaleSkalIkkeUtloseOpsjoner && <RegistrerOpsjonForm avtale={avtale} />}

                {mutation.isError && (
                  <Alert variant="error">Noe gikk galt ved registrering av opsjon</Alert>
                )}
              </BodyLong>
              {avtale.opsjonerRegistrert.length > 0 ? (
                <RegistrerteOpsjoner readOnly={false} />
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

function getNesteSluttDato(
  opsjonsvalg: Opsjonsvalg,
  avtaleSluttDato?: string | null,
  customSluttDato?: string | null,
): string | null {
  switch (opsjonsvalg) {
    case "1":
      return avtaleSluttDato
        ? (yyyyMMddFormatting(addDuration(new Date(avtaleSluttDato), { years: 1 })) ?? null)
        : null;
    case "Annet":
      return customSluttDato || null;
    case "Opsjon_skal_ikke_utloses":
      return null;
  }
}

function getStatus(opsjonsvalg: Opsjonsvalg): OpsjonStatus {
  switch (opsjonsvalg) {
    case "1":
      return OpsjonStatus.OPSJON_UTLOST;
    case "Annet":
      return OpsjonStatus.OPSJON_UTLOST;
    case "Opsjon_skal_ikke_utloses":
      return OpsjonStatus.SKAL_IKKE_UTLOSE_OPSJON;
  }
}
