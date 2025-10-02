import { useSetTilgjengeligForArrangor } from "@/api/gjennomforing/useSetTilgjengeligForArrangor";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { FieldError, ValidationError as LegacyValidationError } from "@mr/api-client-v2";
import {
  GjennomforingDto,
  GjennomforingHandling,
  SetTilgjengligForArrangorRequest,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Alert, Button, Heading, HStack, Modal } from "@navikt/ds-react";
import { useRef } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { formaterDato, maxOf, subDuration } from "@mr/frontend-common/utils/date";
import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";

interface Props {
  gjennomforing: GjennomforingDto;
}

export function TiltakTilgjengeligForArrangor({ gjennomforing }: Props) {
  const modalRef = useRef<HTMLDialogElement>(null);
  const { data: handlinger } = useGjennomforingHandlinger(gjennomforing.id);
  const setTilgjengeligForArrangorMutation = useSetTilgjengeligForArrangor();

  const form = useForm<SetTilgjengligForArrangorRequest>({
    resolver: async (values) => ({ values, errors: {} }),
    defaultValues: {
      tilgjengeligForArrangorDato: gjennomforing.tilgjengeligForArrangorDato,
    },
  });

  const onValidationError = (error: ValidationError | LegacyValidationError) => {
    error.errors.forEach((error: FieldError) => {
      form.setError(
        jsonPointerToFieldPath(error.pointer) as keyof SetTilgjengligForArrangorRequest,
        {
          type: "custom",
          message: error.detail,
        },
      );
    });
  };

  const submit = form.handleSubmit(async (values) => {
    setTilgjengeligForArrangorMutation.mutate(
      {
        id: gjennomforing.id,
        dato: values.tilgjengeligForArrangorDato,
      },
      { onSuccess: () => modalRef.current?.close(), onValidationError },
    );
  });

  const cancel = () => {
    form.reset({});
    modalRef.current?.close();
  };

  const today = new Date();
  const gjennomforingStartDato = new Date(gjennomforing.startDato);

  const tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorDato
    ? new Date(gjennomforing.tilgjengeligForArrangorDato)
    : maxOf([subDuration(gjennomforingStartDato, { days: 14 }), today]);

  const mintilgjengeligForArrangorDato = maxOf([
    subDuration(gjennomforingStartDato, { months: 2 }),
    today,
  ]);

  return (
    <Alert variant="info">
      <Heading level="4" size="small">
        Når ser arrangør tiltaket?
      </Heading>
      <p>
        Arrangør har tilgang til tiltaket i Deltakeroversikten på nav.no fra{" "}
        <b>{formaterDato(tilgjengeligForArrangorDato)}</b>.
      </p>

      {handlinger.includes(GjennomforingHandling.ENDRE_TILGJENGELIG_FOR_ARRANGOR) && (
        <Button size="small" variant="secondary" onClick={() => modalRef.current?.showModal()}>
          Endre dato
        </Button>
      )}

      <Modal
        ref={modalRef}
        header={{ heading: "Når skal arrangør ha tilgang til tiltaket?", closeButton: false }}
        closeOnBackdropClick
      >
        <Modal.Body>
          <FormProvider {...form}>
            <form>
              <HStack gap="2" align={"end"} justify={"center"}>
                <ControlledDateInput
                  label="Når skal arrangør ha tilgang?"
                  size="small"
                  defaultSelected={form.getValues("tilgjengeligForArrangorDato")}
                  onChange={(val) => form.setValue("tilgjengeligForArrangorDato", val)}
                  error={form.formState.errors.tilgjengeligForArrangorDato?.message}
                  fromDate={mintilgjengeligForArrangorDato}
                  toDate={gjennomforingStartDato}
                  invalidDatoEtterPeriode="Du må velge en dato som er før oppstartsdato"
                />
              </HStack>
            </form>
          </FormProvider>
        </Modal.Body>
        <Modal.Footer>
          <Button size="small" type="submit" onClick={submit}>
            Endre dato
          </Button>
          <Button size="small" type="button" variant="secondary" onClick={cancel}>
            Avbryt endring
          </Button>
        </Modal.Footer>
      </Modal>
    </Alert>
  );
}
