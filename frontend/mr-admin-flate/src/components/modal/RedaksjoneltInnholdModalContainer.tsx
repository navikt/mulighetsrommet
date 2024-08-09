import { PropsWithChildren } from "react";
import { Modal } from "@navikt/ds-react";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  closeOnBackdropClick?: boolean;
}
export function RedaksjoneltInnholdModalContainer({
  modalOpen,
  onClose,
  closeOnBackdropClick,
  children,
}: PropsWithChildren<Props>) {
  return (
    <Modal
      open={modalOpen}
      onClose={() => {
        onClose();
      }}
      style={{ maxHeight: "70rem" }}
      aria-label="modal"
      width="50rem"
      closeOnBackdropClick={closeOnBackdropClick}
    >
      {children}
    </Modal>
  );
}
