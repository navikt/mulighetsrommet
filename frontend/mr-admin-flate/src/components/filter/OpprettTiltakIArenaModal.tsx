import { useMigrerteTiltakstyper } from "@/api/tiltakstyper/useMigrerteTiltakstyper";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { BodyShort, Heading, Modal, VStack } from "@navikt/ds-react";
import styles from "@/components/modal/Modal.module.scss";

interface OpprettTiltakIArenaModalProps {
  open: boolean;
  onClose: () => void;
  tiltakstype: string;
}

export function OpprettTiltakIArenaModal({
  open,
  onClose,
  tiltakstype,
}: OpprettTiltakIArenaModalProps) {
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyper();
  const { data: tiltakstyper } = useTiltakstyper();

  const migrerteTiltakstyperNavn =
    tiltakstyper?.data
      .filter((tiltakstype) => migrerteTiltakstyper?.includes(tiltakstype.arenaKode))
      .map((tiltakstype) => tiltakstype.navn) ?? [];

  return (
    <Modal
      open={open}
      onClose={onClose}
      closeOnBackdropClick
      className={styles.modal_container}
      aria-label="modal"
      width="50rem"
    >
      <Modal.Header closeButton>
        <Heading size="medium">Tiltaksgjennomføring kan ikke opprettes her</Heading>
      </Modal.Header>
      <Modal.Body className={styles.modal_content}>
        <VStack gap="2">
          <BodyShort>
            Tiltak knyttet til tiltakstypen <code>{tiltakstype}</code> kan ikke opprettes her enda.
            Du må fortsatt opprette tiltaksgjennomføringer for denne tiltakstypen i Arena.
          </BodyShort>
          {migrerteTiltakstyperNavn.length > 0 ? (
            <>
              <Heading size="small" level="4">
                Du kan opprette tiltak for følgende tiltakstyper her i NAV Tiltaksadministrasjon:
              </Heading>
              <ul>
                {migrerteTiltakstyperNavn.map((tiltakstype) => (
                  <li key={tiltakstype}>{tiltakstype}</li>
                ))}
              </ul>
            </>
          ) : null}
        </VStack>
      </Modal.Body>
    </Modal>
  );
}
