import { PropsWithChildren } from "react";
import { Modal } from "@navikt/ds-react";

export function RedaksjoneltInnholdModalBody(props: PropsWithChildren) {
  return (
    <Modal.Body style={{ display: "flex", flexDirection: "column", gap: "2rem" }}>
      {props.children}
    </Modal.Body>
  );
}
