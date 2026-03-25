import { useSetTilgjengeligForArrangor } from "@/api/gjennomforing/useSetTilgjengeligForArrangor";
import {
  GjennomforingAvtaleDto,
  GjennomforingHandling,
  SetTilgjengligForArrangorRequest,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { BodyShort, Button, Dialog, HStack, InfoCard } from "@navikt/ds-react";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { formaterDato, maxOf, subDuration } from "@mr/frontend-common/utils/date";
import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { applyValidationErrors } from "@/components/skjema/helpers";

interface Props {
  gjennomforing: GjennomforingAvtaleDto;
}

export function TiltakTilgjengeligForArrangor({ gjennomforing }: Props) {
  const [open, setOpen] = useState(false);

  const handlinger = useGjennomforingHandlinger(gjennomforing.id);
  const setTilgjengeligForArrangorMutation = useSetTilgjengeligForArrangor(gjennomforing.id);

  const form = useForm<SetTilgjengligForArrangorRequest>({
    defaultValues: toDefaults(gjennomforing),
  });

  const submit = form.handleSubmit(async (values) => {
    setTilgjengeligForArrangorMutation.mutate(values, {
      onSuccess: () => setOpen(false),
      onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
    });
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

  const id = "form-tilgjengelig-for-arrangor";

  return (
    <InfoCard>
      <InfoCard.Header>
        <InfoCard.Title>Når ser arrangør tiltaket?</InfoCard.Title>
      </InfoCard.Header>
      <InfoCard.Content>
        <BodyShort spacing>
          Arrangør har tilgang til tiltaket i Deltakeroversikten på nav.no fra{" "}
          <b>{formaterDato(tilgjengeligForArrangorDato)}</b>.
        </BodyShort>

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
                <form id={id} onSubmit={submit}>
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
              <Button form={id} size="small">
                Endre dato
              </Button>
            </Dialog.Footer>
          </Dialog.Popup>
        </Dialog>
      </InfoCard.Content>
    </InfoCard>
  );
}

function toDefaults(gjennomforing: GjennomforingAvtaleDto): SetTilgjengligForArrangorRequest {
  return { tilgjengeligForArrangorDato: gjennomforing.tilgjengeligForArrangorDato };
}
