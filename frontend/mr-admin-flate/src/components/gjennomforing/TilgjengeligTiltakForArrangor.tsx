import { useSetTilgjengeligForArrangor } from "@/api/gjennomforing/useSetTilgjengeligForArrangor";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { formaterDato, max, subtractDays, subtractMonths } from "@/utils/Utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { FieldError, GjennomforingDto, ValidationError } from "@mr/api-client-v2";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Alert, Button, Heading, HStack, Modal } from "@navikt/ds-react";
import { useRef } from "react";
import { FormProvider, useForm } from "react-hook-form";
import z from "zod";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

interface Props {
  gjennomforing: GjennomforingDto;
}

export const EditTilgjengeligForArrangorSchema = z.object({
  tilgjengeligForArrangorDato: z.string({ required_error: "Feltet er påkrevd" }).date(),
});

export type InferredEditTilgjengeligForArrangorSchema = z.infer<
  typeof EditTilgjengeligForArrangorSchema
>;

export function TiltakTilgjengeligForArrangor({ gjennomforing }: Props) {
  const modalRef = useRef<HTMLDialogElement>(null);

  const form = useForm<InferredEditTilgjengeligForArrangorSchema>({
    resolver: zodResolver(EditTilgjengeligForArrangorSchema),
    defaultValues: {
      tilgjengeligForArrangorDato: gjennomforing.tilgjengeligForArrangorDato ?? undefined,
    },
  });

  const setTilgjengeligForArrangorMutation = useSetTilgjengeligForArrangor();

  const onSuccess = () => {
    modalRef.current?.close();
  };

  const onValidationError = (error: ValidationError) => {
    error.errors.forEach((error: FieldError) => {
      form.setError(
        jsonPointerToFieldPath(error.pointer) as keyof InferredEditTilgjengeligForArrangorSchema,
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
        dato: values.tilgjengeligForArrangorDato!,
      },
      { onSuccess, onValidationError },
    );
  });

  const cancel = () => {
    form.reset({});
    modalRef.current?.close();
  };

  const dagensDato = new Date();

  const gjennomforingStartDato = new Date(gjennomforing.startDato);

  const tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorDato
    ? new Date(gjennomforing.tilgjengeligForArrangorDato)
    : max(subtractDays(gjennomforingStartDato, 14), dagensDato);

  const mintilgjengeligForArrangorDato = max(subtractMonths(gjennomforingStartDato, 2), dagensDato);

  if (dagensDato > gjennomforingStartDato) {
    return null;
  }

  return (
    <Alert variant="info">
      <Heading level="4" size="small">
        Når ser arrangør tiltaket?
      </Heading>

      <TilgjengeligForArrangorInfo tilgjengeligForArrangorDato={tilgjengeligForArrangorDato} />

      <HarSkrivetilgang ressurs="Gjennomføring">
        <Button size="small" variant="secondary" onClick={() => modalRef.current?.showModal()}>
          Endre dato
        </Button>
      </HarSkrivetilgang>

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
                  fromDate={mintilgjengeligForArrangorDato}
                  toDate={gjennomforingStartDato}
                  format="iso-string"
                  invalidDatoEtterPeriode="Du må velge en dato som er før oppstartsdato"
                  {...form.register("tilgjengeligForArrangorDato")}
                  control={form.control}
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

function TilgjengeligForArrangorInfo({
  tilgjengeligForArrangorDato,
}: {
  tilgjengeligForArrangorDato: Date;
}) {
  return (
    <p>
      Arrangør har tilgang til tiltaket i Deltakeroversikten på nav.no fra{" "}
      <b>{formaterDato(tilgjengeligForArrangorDato)}</b>.
    </p>
  );
}
