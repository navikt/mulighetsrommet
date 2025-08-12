import { UtbetalingLinje } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { InformationSquareFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Modal } from "@navikt/ds-react";

export default function AttesterDelutbetalingModal({
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
