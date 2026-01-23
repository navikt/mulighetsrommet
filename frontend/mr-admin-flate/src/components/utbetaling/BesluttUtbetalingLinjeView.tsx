import { useAttesterDelutbetaling, useReturnerDelutbetaling } from "@/api/utbetaling/mutations";
import {
  AarsakerOgForklaringRequestDelutbetalingReturnertAarsak,
  DelutbetalingReturnertAarsak,
  FieldError,
  UtbetalingDto,
  UtbetalingLinje,
  UtbetalingLinjeHandling,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { BodyShort, Button, Heading, HStack, TextField, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import AttesterDelutbetalingModal from "./AttesterDelutbetalingModal";
import { isBesluttet } from "@/utils/totrinnskontroll";
import { useUtbetalingsLinjer } from "@/pages/gjennomforing/utbetaling/utbetalingPageLoader";
import { utbetalingTekster } from "./UtbetalingTekster";
import { GjorOppTilsagnCheckbox } from "./GjorOppTilsagnCheckbox";
import { useRequiredParams } from "@/hooks/useRequiredParams";

export interface Props {
  utbetaling: UtbetalingDto;
  oppdaterLinjer: () => Promise<void>;
}

export function BesluttUtbetalingLinjeView({ utbetaling, oppdaterLinjer }: Props) {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: linjer } = useUtbetalingsLinjer(utbetaling.id);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const [errors, setErrors] = useState<FieldError[]>([]);
  const attesterDelutbetalingMutation = useAttesterDelutbetaling();
  const returnerDelutbetalingMutation = useReturnerDelutbetaling();

  function attesterDelutbetaling(id: string) {
    attesterDelutbetalingMutation.mutate(
      { id },
      {
        onSuccess: oppdaterLinjer,
        onValidationError: (error: ValidationError) => {
          setErrors(error.errors);
        },
      },
    );
  }

  function returnerDelutbetaling(
    id: string,
    body: AarsakerOgForklaringRequestDelutbetalingReturnertAarsak,
  ) {
    returnerDelutbetalingMutation.mutate(
      { id, body },
      {
        onSuccess: oppdaterLinjer,
        onValidationError: (error: ValidationError) => {
          setErrors(error.errors);
        },
      },
    );
  }

  function openRow(linje: UtbetalingLinje): boolean {
    const hasNonBelopErrors = errors.filter((e) => !e.pointer.includes("belop"));
    return hasNonBelopErrors.length > 0 || isBesluttet(linje.opprettelse);
  }

  const returnerAarsakValg = [
    DelutbetalingReturnertAarsak.FEIL_BELOP,
    DelutbetalingReturnertAarsak.ANNET,
  ].map((val) => {
    return {
      value: val,
      label: utbetalingTekster.delutbetaling.aarsak.fraRetunertAarsak(val),
    };
  });

  return (
    <VStack gap="2">
      <Heading spacing size="medium">
        {utbetalingTekster.delutbetaling.header}
      </Heading>
      <UtbetalingLinjeTable
        linjer={linjer.filter((l) => l.status !== null)}
        utbetaling={utbetaling}
        renderRow={(linje) => {
          return (
            <UtbetalingLinjeRow
              key={`${linje.id}-${linje.status?.type}`}
              gjennomforingId={gjennomforingId}
              linje={linje}
              grayBackground
              rowOpen={openRow(linje)}
              checkboxInput={<GjorOppTilsagnCheckbox linje={linje} />}
              belopInput={
                <TextField
                  readOnly
                  value={linje.pris.belop}
                  size="small"
                  style={{ maxWidth: "6rem" }}
                  hideLabel
                  type="number"
                  label={utbetalingTekster.delutbetaling.belop.label}
                />
              }
              knappeColumn={
                <HStack gap="4">
                  {linje.handlinger.includes(UtbetalingLinjeHandling.RETURNER) && (
                    <Button
                      variant="secondary"
                      size="small"
                      type="button"
                      onClick={() => setAvvisModalOpen(true)}
                    >
                      {utbetalingTekster.delutbetaling.handlinger.returner}
                    </Button>
                  )}
                  {linje.handlinger.includes(UtbetalingLinjeHandling.ATTESTER) && (
                    <Button
                      key={`attester-knapp-${linje.id}`}
                      size="small"
                      type="button"
                      onClick={() => {
                        const modal = document.getElementById(
                          `godkjenn-modal-${linje.id}`,
                        ) as HTMLDialogElement;
                        modal.showModal();
                      }}
                    >
                      {utbetalingTekster.delutbetaling.handlinger.attester}
                    </Button>
                  )}
                  <AarsakerOgForklaringModal<DelutbetalingReturnertAarsak>
                    header={utbetalingTekster.delutbetaling.aarsak.modal.header}
                    ingress={
                      <BodyShort>{utbetalingTekster.delutbetaling.aarsak.modal.ingress}</BodyShort>
                    }
                    open={avvisModalOpen}
                    buttonLabel={utbetalingTekster.delutbetaling.aarsak.modal.button.label}
                    errors={errors}
                    aarsaker={returnerAarsakValg}
                    onClose={() => {
                      setAvvisModalOpen(false);
                      setErrors([]);
                    }}
                    onConfirm={(request) => {
                      returnerDelutbetaling(linje.id, request);
                      setAvvisModalOpen(false);
                    }}
                  />
                  <AttesterDelutbetalingModal
                    id={`godkjenn-modal-${linje.id}`}
                    handleClose={() => {
                      const modal = document.getElementById(
                        `godkjenn-modal-${linje.id}`,
                      ) as HTMLDialogElement;
                      modal.close();
                    }}
                    onConfirm={() => {
                      const modal = document.getElementById(
                        `godkjenn-modal-${linje.id}`,
                      ) as HTMLDialogElement;
                      modal.close();
                      attesterDelutbetaling(linje.id);
                    }}
                    linje={linje}
                  />
                </HStack>
              }
            />
          );
        }}
      />
    </VStack>
  );
}
