import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import {
  BesluttDelutbetalingRequest,
  Besluttelse,
  DelutbetalingReturnertAarsak,
  DelutbetalingStatus,
  FieldError,
  UtbetalingDto,
  UtbetalingLinje,
  ValidationError,
} from "@mr/api-client-v2";
import { InformationSquareFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading, HStack, Modal, VStack } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
}

export function BesluttUtbetalingLinjeView({ linjer, utbetaling }: Props) {
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const queryClient = useQueryClient();
  const [errors, setErrors] = useState<FieldError[]>([]);
  const besluttMutation = useBesluttDelutbetaling();

  function beslutt(id: string, body: BesluttDelutbetalingRequest) {
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
                linje?.status === DelutbetalingStatus.TIL_ATTESTERING && (
                  <HStack gap="4">
                    <Button
                      variant="secondary"
                      size="small"
                      type="button"
                      onClick={() => setAvvisModalOpen(true)}
                    >
                      Send i retur
                    </Button>
                    {linje?.opprettelse?.kanBesluttes && (
                      <Button
                        size="small"
                        type="button"
                        onClick={() => {
                          const modal = document.getElementById(
                            `godkjenn-modal-${linje.id}`,
                          ) as HTMLDialogElement;
                          modal?.showModal();
                        }}
                      >
                        Attester
                      </Button>
                    )}
                    <AarsakerOgForklaringModal<DelutbetalingReturnertAarsak>
                      ingress="Ved å sende en linje i retur vil andre linjer også returneres"
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
                        modal?.close();
                      }}
                      onConfirm={() => {
                        const modal = document.getElementById(
                          `godkjenn-modal-${linje.id}`,
                        ) as HTMLDialogElement;
                        modal?.close();
                        beslutt(linje.id, {
                          besluttelse: Besluttelse.GODKJENT,
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

function AttesterDelutbetalingModal({
  id,
  handleClose,
  onConfirm,
  linje,
}: {
  id: string;
  handleClose: () => void;
  onConfirm: () => void;
  linje: UtbetalingLinje;
}) {
  return (
    <Modal
      className="text-left"
      id={id}
      onClose={handleClose}
      header={{
        heading: "Attestere utbetaling",
        icon: <InformationSquareFillIcon />,
      }}
    >
      <Modal.Body>
        <BodyShort>
          Du er i ferd med å attestere utbetalingsbeløp {formaterNOK(linje.belop)} for kostnadssted{" "}
          {linje.tilsagn.kostnadssted.navn}. Er du sikker?
        </BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="primary" onClick={onConfirm}>
          Ja, attester beløp
        </Button>
        <Button variant="secondary" onClick={handleClose}>
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
