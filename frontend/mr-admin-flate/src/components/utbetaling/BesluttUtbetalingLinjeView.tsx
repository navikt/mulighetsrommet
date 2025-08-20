import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import {
  Besluttelse,
  BesluttTotrinnskontrollRequest,
  DelutbetalingReturnertAarsak,
  DelutbetalingStatus,
  FieldError,
  UtbetalingDto,
  UtbetalingLinje,
  ValidationError,
} from "@mr/api-client-v2";
import { BodyShort, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import AttesterDelutbetalingModal from "./AttesterDelutbetalingModal";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
}

export function BesluttUtbetalingLinjeView({ linjer, utbetaling }: Props) {
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const queryClient = useQueryClient();
  const [errors, setErrors] = useState<FieldError[]>([]);
  const besluttMutation = useBesluttDelutbetaling();

  function beslutt(id: string, body: BesluttTotrinnskontrollRequest) {
    besluttMutation.mutate(
      { id, body },
      {
        onSuccess: () => {
          return queryClient.invalidateQueries({ queryKey: ["utbetaling"] });
        },
        onValidationError: (error: ValidationError) => {
          setErrors(error.errors);
        },
      },
    );
  }

  return (
    <VStack gap="2">
      <Heading spacing size="medium">
        Utbetalingslinjer
      </Heading>
      <UtbetalingLinjeTable
        linjer={linjer}
        utbetaling={utbetaling}
        renderRow={(linje) => {
          return (
            <UtbetalingLinjeRow
              readOnly
              key={linje.id}
              linje={linje}
              grayBackground
              knappeColumn={
                linje.status === DelutbetalingStatus.TIL_ATTESTERING && (
                  <HStack gap="4">
                    <Button
                      variant="secondary"
                      size="small"
                      type="button"
                      onClick={() => setAvvisModalOpen(true)}
                    >
                      Send i retur
                    </Button>
                    {linje.opprettelse?.kanBesluttes && (
                      <Button
                        size="small"
                        type="button"
                        onClick={() => {
                          const modal = document.getElementById(
                            `godkjenn-modal-${linje.id}`,
                          ) as HTMLDialogElement;
                          modal.showModal();
                        }}
                      >
                        Attester
                      </Button>
                    )}
                    <AarsakerOgForklaringModal<DelutbetalingReturnertAarsak>
                      ingress={
                        <BodyShort>
                          Ved å sende en linje i retur vil andre linjer også returneres
                        </BodyShort>
                      }
                      open={avvisModalOpen}
                      header="Send i retur med forklaring"
                      buttonLabel="Send i retur"
                      errors={errors}
                      aarsaker={[
                        { value: DelutbetalingReturnertAarsak.FEIL_BELOP, label: "Feil beløp" },
                        { value: DelutbetalingReturnertAarsak.ANNET, label: "Annet" },
                      ]}
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
