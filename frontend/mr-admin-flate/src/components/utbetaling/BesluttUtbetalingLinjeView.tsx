import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import { isValidationError } from "@/utils/Utils";
import {
  BesluttDelutbetalingRequest,
  Besluttelse,
  DelutbetalingReturnertAarsak,
  DelutbetalingStatus,
  FieldError,
  ProblemDetail,
  UtbetalingDto,
  UtbetalingLinje,
} from "@mr/api-client-v2";
import { InformationSquareFillIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, Heading, HStack, Modal, VStack } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useRef, useState } from "react";
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
  const [error, setError] = useState<FieldError[]>([]);
  const godkjennDelutbetalingModalRef = useRef<HTMLDialogElement | null>(null);

  const besluttMutation = useBesluttDelutbetaling();

  function beslutt(id: string, body: BesluttDelutbetalingRequest) {
    besluttMutation.mutate(
      { id, body },
      {
        onSuccess: () => {
          return queryClient.invalidateQueries({ queryKey: ["utbetaling"] });
        },
        onError: (error: ProblemDetail) => {
          if (isValidationError(error)) {
            setError(error.errors);
          } else {
            throw error;
          }
        },
      },
    );
  }

  function visBekreftGodkjennModal() {
    godkjennDelutbetalingModalRef?.current?.showModal();
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
                linje?.status === DelutbetalingStatus.TIL_GODKJENNING &&
                linje?.opprettelse?.kanBesluttes && (
                  <HStack gap="4">
                    <Button
                      size="small"
                      type="button"
                      onClick={() => {
                        visBekreftGodkjennModal();
                      }}
                    >
                      Godkjenn
                    </Button>
                    <Button
                      variant="secondary"
                      size="small"
                      type="button"
                      onClick={() => setAvvisModalOpen(true)}
                    >
                      Send i retur
                    </Button>
                    <AarsakerOgForklaringModal<DelutbetalingReturnertAarsak>
                      open={avvisModalOpen}
                      header="Send i retur med forklaring"
                      buttonLabel="Send i retur"
                      aarsaker={[
                        { value: DelutbetalingReturnertAarsak.FEIL_BELOP, label: "Feil beløp" },
                        { value: DelutbetalingReturnertAarsak.FEIL_ANNET, label: "Annet" },
                      ]}
                      onClose={() => setAvvisModalOpen(false)}
                      onConfirm={({ aarsaker, forklaring }) => {
                        beslutt(linje.id, {
                          besluttelse: Besluttelse.AVVIST,
                          aarsaker,
                          forklaring: forklaring ?? null,
                        });
                        setAvvisModalOpen(false);
                      }}
                    />
                    <GodkjennDelutbetalingModal
                      ref={godkjennDelutbetalingModalRef}
                      handleClose={() => godkjennDelutbetalingModalRef?.current?.close()}
                      onConfirm={() => {
                        godkjennDelutbetalingModalRef?.current?.close();
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
      {error.find((f) => f.pointer === "/") && (
        <Alert className="self-end" variant="error" size="small">
          {error.find((f) => f.pointer === "/")!.detail}
        </Alert>
      )}
    </VStack>
  );
}

function GodkjennDelutbetalingModal({
  ref,
  handleClose,
  onConfirm,
  linje,
}: {
  ref: React.RefObject<HTMLDialogElement | null>;
  handleClose: () => void;
  onConfirm: () => void;
  linje: UtbetalingLinje;
}) {
  return (
    <Modal
      className="text-left"
      ref={ref}
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
