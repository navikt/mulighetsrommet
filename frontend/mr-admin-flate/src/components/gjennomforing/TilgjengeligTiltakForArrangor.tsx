import { useSetTilgjengeligForArrangor } from "@/api/gjennomforing/useSetTilgjengeligForArrangor";
import {
  FieldError,
  GjennomforingAvtaleDto,
  GjennomforingHandling,
  SetTilgjengligForArrangorRequest,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Alert, Button, Dialog, Heading, HStack } from "@navikt/ds-react";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { formaterDato, maxOf, subDuration } from "@mr/frontend-common/utils/date";
import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { FormDateInput } from "@/components/skjema/FormDateInput";

interface Props {
  gjennomforing: GjennomforingAvtaleDto;
}

export function TiltakTilgjengeligForArrangor({ gjennomforing }: Props) {
  const [open, setOpen] = useState(false);

  const handlinger = useGjennomforingHandlinger(gjennomforing.id);
  const setTilgjengeligForArrangorMutation = useSetTilgjengeligForArrangor();

  const form = useForm<SetTilgjengligForArrangorRequest>({
    defaultValues: toDefaults(gjennomforing),
  });

  const onValidationError = (error: ValidationError) => {
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
      { onSuccess: () => setOpen(false), onValidationError },
    );
  });

  const resetForm = () => {
    setOpen(false);
    form.reset(toDefaults(gjennomforing));
  };

  const onOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      resetForm();
    }
  };

  const gjennomforingStartDato = new Date(gjennomforing.startDato);

  const today = new Date();
  const tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorDato
    ? new Date(gjennomforing.tilgjengeligForArrangorDato)
    : maxOf([subDuration(gjennomforingStartDato, { days: 14 }), today]);

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
        <Button size="small" variant="secondary" onClick={() => setOpen(true)}>
          Endre dato
        </Button>
      )}

      <Dialog open={open} onOpenChange={onOpenChange}>
        <Dialog.Popup>
          <Dialog.Header withClosebutton={false}>
            <Dialog.Title>Når skal arrangør ha tilgang til tiltaket?</Dialog.Title>
          </Dialog.Header>
          <Dialog.Body>
            <FormProvider {...form}>
              <form id="form-tilgjengelig-for-arrangor" onSubmit={submit}>
                <HStack gap="space-8" align={"end"} justify={"center"}>
                  <FormDateInput<SetTilgjengligForArrangorRequest>
                    label="Når skal arrangør ha tilgang?"
                    name="tilgjengeligForArrangorDato"
                    fromDate={today}
                    toDate={gjennomforingStartDato}
                  />
                </HStack>
              </form>
            </FormProvider>
          </Dialog.Body>
          <Dialog.Footer>
            <Button type="button" variant="secondary" size="small" onClick={resetForm}>
              Avbryt
            </Button>
            <Button form="form-tilgjengelig-for-arrangor" size="small">
              Endre dato
            </Button>
          </Dialog.Footer>
        </Dialog.Popup>
      </Dialog>
    </Alert>
  );
}

function toDefaults(gjennomforing: GjennomforingAvtaleDto): SetTilgjengligForArrangorRequest {
  return { tilgjengeligForArrangorDato: gjennomforing.tilgjengeligForArrangorDato };
}
