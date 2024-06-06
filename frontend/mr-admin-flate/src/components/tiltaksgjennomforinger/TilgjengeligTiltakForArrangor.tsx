import { Alert, Button, Heading, HStack, Modal } from "@navikt/ds-react";
import { useRef } from "react";
import { formaterDato, max, subtractDays, subtractMonths } from "@/utils/Utils";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { FormProvider, useForm } from "react-hook-form";
import z from "zod";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { zodResolver } from "@hookform/resolvers/zod";
import { useSetTilgjengeligForArrangor } from "@/api/tiltaksgjennomforing/useSetTilgjengeligForArrangor";
import { useHandleApiUpsertResponse } from "@/api/effects";

interface Props {
  gjennomforing: Tiltaksgjennomforing;
}

export const EditTilgjengeligForArrangorSchema = z.object({
  tilgjengeligForArrangorFraOgMedDato: z.string({ required_error: "Feltet er påkrevd" }).date(),
});

export type InferredEditTilgjengeligForArrangorSchema = z.infer<
  typeof EditTilgjengeligForArrangorSchema
>;

export function TiltakTilgjengeligForArrangor({ gjennomforing }: Props) {
  const modalRef = useRef<HTMLDialogElement>(null);

  const form = useForm<Partial<InferredEditTilgjengeligForArrangorSchema>>({
    resolver: zodResolver(EditTilgjengeligForArrangorSchema),
    defaultValues: {
      tilgjengeligForArrangorFraOgMedDato:
        gjennomforing.tilgjengeligForArrangorFraOgMedDato ?? undefined,
    },
  });

  const setTilgjengeligForArrangor = useSetTilgjengeligForArrangor();

  useHandleApiUpsertResponse(
    setTilgjengeligForArrangor,
    () => {
      modalRef.current?.close();
    },
    (validation) => {
      validation.errors.forEach((error) => {
        form.setError(error.name as keyof InferredEditTilgjengeligForArrangorSchema, {
          type: "custom",
          message: error.message,
        });
      });
    },
  );

  const submit = form.handleSubmit(async (values) => {
    setTilgjengeligForArrangor.mutate({
      id: gjennomforing.id,
      tilgjengeligForArrangorDato: values.tilgjengeligForArrangorFraOgMedDato!,
    });
  });

  const cancel = () => {
    form.reset({});
    modalRef.current?.close();
  };

  const dagensDato = new Date();

  const gjennomforingStartDato = new Date(gjennomforing.startDato);

  const tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorFraOgMedDato
    ? new Date(gjennomforing.tilgjengeligForArrangorFraOgMedDato)
    : max(subtractDays(gjennomforingStartDato, 14), dagensDato);

  const minTilgjengeligForArrangorFraOgMedDato = max(
    subtractMonths(gjennomforingStartDato, 2),
    dagensDato,
  );

  if (dagensDato > gjennomforingStartDato) {
    return null;
  }

  return (
    <Alert variant="info">
      <Heading level="4" size="small">
        Når ser arrangør tiltaket?
      </Heading>

      <TilgjengeligForArrangorInfo tilgjengeligForArrangorDato={tilgjengeligForArrangorDato} />

      <Button size="small" variant="secondary" onClick={() => modalRef.current?.showModal()}>
        Endre dato
      </Button>

      <Modal
        ref={modalRef}
        header={{ heading: "Når skal arrangør ha tilgang til tiltaket?", closeButton: false }}
      >
        <Modal.Body>
          <FormProvider {...form}>
            <form>
              <HStack gap="2" align={"end"}>
                <ControlledDateInput
                  label="Når skal arrangør ha tilgang?"
                  size="small"
                  fromDate={minTilgjengeligForArrangorFraOgMedDato}
                  toDate={gjennomforingStartDato}
                  format="iso-string"
                  invalidDatoEtterPeriode="Du må velge en dato som er før oppstartsdato"
                  {...form.register("tilgjengeligForArrangorFraOgMedDato")}
                />
              </HStack>
            </form>
          </FormProvider>
        </Modal.Body>

        <Modal.Footer>
          <Button type="submit" onClick={submit}>
            Endre dato
          </Button>
          <Button type="button" variant="secondary" onClick={cancel}>
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
