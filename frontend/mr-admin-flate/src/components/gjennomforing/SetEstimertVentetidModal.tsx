import { useSetEstimertVentetid } from "@/api/gjennomforing/useSetEstimertVentetid";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormSwitch } from "@/components/skjema/FormSwitch";
import { NumberInput } from "@/components/skjema/NumberInput";
import { ValideringsfeilOppsummering } from "@/components/skjema/ValideringsfeilOppsummering";
import { BodyShort, Button, HStack, Modal } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { ValidationError } from "@tiltaksadministrasjon/api-client";

interface Props {
  open: boolean;
  setOpen(open: boolean): void;
  gjennomforingId: string;
}

interface EstimertVentetidFormValues {
  enableEstimertVentetid: boolean;
  verdi: number | undefined;
  enhet: string;
}

const formId = "set-estimert-ventetid-form";

export function SetEstimertVentetidModal({ open, setOpen, gjennomforingId }: Props) {
  const { veilederinfo } = useGjennomforing(gjennomforingId);
  const mutation = useSetEstimertVentetid(gjennomforingId);

  const form = useForm<EstimertVentetidFormValues>({
    values: {
      enableEstimertVentetid: !!veilederinfo?.estimertVentetid,
      verdi: veilederinfo?.estimertVentetid?.verdi,
      enhet: veilederinfo?.estimertVentetid?.enhet ?? "uke",
    },
  });

  function closeAndReset() {
    mutation.reset();
    setOpen(false);
  }

  const postData: SubmitHandler<EstimertVentetidFormValues> = (data) => {
    const payload = data.enableEstimertVentetid
      ? { verdi: data.verdi ?? null, enhet: data.enhet }
      : { verdi: null, enhet: null };

    mutation.mutate(payload, {
      onSuccess: closeAndReset,
      onValidationError: (validation: ValidationError) => applyValidationErrors(form, validation),
    });
  };

  const enableEstimertVentetid = form.watch("enableEstimertVentetid");

  return (
    <Modal
      open={open}
      onClose={closeAndReset}
      header={{ heading: "Estimert ventetid" }}
      width="medium"
    >
      <FormProvider {...form}>
        <Modal.Body>
          <BodyShort spacing>
            Her kan du registrere estimert ventetid for tiltaket. Informasjonen blir synlig for
            veiledere i Modia.
          </BodyShort>
          <form id={formId} onSubmit={form.handleSubmit(postData)}>
            <FormSwitch<EstimertVentetidFormValues> name="enableEstimertVentetid">
              Registrer estimert ventetid
            </FormSwitch>
            {enableEstimertVentetid && (
              <HStack align="start" justify="start" gap="space-40">
                <NumberInput<EstimertVentetidFormValues> name="verdi" label="Antall" min={1} />
                <FormSelect<EstimertVentetidFormValues> name="enhet" label="Måleenhet">
                  <option value="uke">Uker</option>
                  <option value="maned">Måneder</option>
                </FormSelect>
              </HStack>
            )}
          </form>
        </Modal.Body>
        <Modal.Footer>
          <Button size="small" form={formId} disabled={mutation.isPending}>
            {mutation.isPending ? "Lagrer..." : "Lagre"}
          </Button>
          <Button size="small" type="button" variant="tertiary" onClick={closeAndReset}>
            Avbryt
          </Button>
          <ValideringsfeilOppsummering />
        </Modal.Footer>
      </FormProvider>
    </Modal>
  );
}
