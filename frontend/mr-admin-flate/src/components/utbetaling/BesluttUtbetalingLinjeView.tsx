import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import {
  Besluttelse,
  BesluttTotrinnskontrollRequestDelutbetalingReturnertAarsak,
  DelutbetalingReturnertAarsak,
  FieldError,
  UtbetalingDto,
  UtbetalingLinje,
  UtbetalingLinjeHandling,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { BodyShort, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import AttesterDelutbetalingModal from "./AttesterDelutbetalingModal";
import { isBesluttet, isTilBeslutning } from "@/utils/totrinnskontroll";
import { useUtbetalingsLinjer } from "@/pages/gjennomforing/utbetaling/utbetalingPageLoader";
import { utbetalingTekster } from "./UtbetalingTekster";
import { GjorOppTilsagnCheckbox } from "./GjorOppTilsagnCheckbox";
import { UtbetalingBelopInput } from "./UtbetalingBelopInput";
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
  const besluttMutation = useBesluttDelutbetaling();

  function beslutt(id: string, body: BesluttTotrinnskontrollRequestDelutbetalingReturnertAarsak) {
    besluttMutation.mutate(
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
        linjer={linjer}
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
              textInput={<UtbetalingBelopInput type="readOnly" linje={linje} />}
              knappeColumn={
                isTilBeslutning(linje.opprettelse) && (
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
                        <BodyShort>
                          {utbetalingTekster.delutbetaling.aarsak.modal.ingress}
                        </BodyShort>
                      }
                      open={avvisModalOpen}
                      buttonLabel={utbetalingTekster.delutbetaling.aarsak.modal.button.label}
                      errors={errors}
                      aarsaker={returnerAarsakValg}
                      onClose={() => {
                        setAvvisModalOpen(false);
                        setErrors([]);
                      }}
                      onConfirm={({ aarsaker, forklaring }) => {
                        beslutt(linje.id, {
                          besluttelse: Besluttelse.AVVIST,
                          aarsaker,
                          forklaring: forklaring ?? null,
                        });
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
                        beslutt(linje.id, {
                          besluttelse: Besluttelse.GODKJENT,
                          aarsaker: [],
                          forklaring: null,
                        });
                      }}
                      linje={linje}
                    />
                  </HStack>
                )
              }
            />
          );
        }}
      />
    </VStack>
  );
}
