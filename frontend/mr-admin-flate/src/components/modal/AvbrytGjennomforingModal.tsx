import { useAvbrytTiltaksgjennomforing } from "@/api/tiltaksgjennomforing/useAvbrytTiltaksgjennomforing";
import styles from "./AvbrytGjennomforingAvtaleModal.module.scss";
import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import classNames from "classnames";
import { Alert, Button, Heading, Modal, Radio } from "@navikt/ds-react";
import { AvbrytGjennomforingRequest, Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { RefObject, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTiltaksgjennomforingDeltakerSummary } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingDeltakerSummary";
import { AvbrytModalError } from "@/components/modal/AvbrytModalError";
import { AvbrytModalAarsaker } from "@/components/modal/AvbrytModalAarsaker";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export const AvbrytGjennomforingModal = ({ modalRef, tiltaksgjennomforing }: Props) => {
  const mutation = useAvbrytTiltaksgjennomforing();
  const navigate = useNavigate();
  const { data: deltakerSummary } = useTiltaksgjennomforingDeltakerSummary(tiltaksgjennomforing.id);

  const [aarsak, setAarsak] = useState<string | null>(null);
  const [customAarsak, setCustomAarsak] = useState<string | null>(null);

  const onClose = () => {
    mutation.reset();
    modalRef.current?.close();
  };

  useEffect(() => {
    modalRef.current?.close();
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}`);
  }, [mutation.isSuccess]);

  const handleAvbrytGjennomforing = () => {
    mutation.reset();
    mutation.mutate({
      id: tiltaksgjennomforing.id,
      aarsak: aarsak === "annet" ? customAarsak : aarsak,
    });
  };

  const aarsakToString = (aarsak: AvbrytGjennomforingRequest.aarsak): string => {
    switch (aarsak) {
      case AvbrytGjennomforingRequest.aarsak.AVBRUTT_I_ARENA:
        return "Avbrutt i Arena";
      case AvbrytGjennomforingRequest.aarsak.BUDSJETT_HENSYN:
        return "Budsjetthensyn";
      case AvbrytGjennomforingRequest.aarsak.ENDRING_HOS_ARRANGOR:
        return "Endring hos arrangør";
      case AvbrytGjennomforingRequest.aarsak.FEILREGISTRERING:
        return "Feilregistrering";
      case AvbrytGjennomforingRequest.aarsak.FOR_FAA_DELTAKERE:
        return "For få deltakere";
    }
  };

  return (
    <Modal ref={modalRef} onClose={onClose} closeOnBackdropClick aria-label="modal">
      <Modal.Header closeButton={false}>
        <div className={styles.heading}>
          <XMarkOctagonFillIcon className={classNames(styles.icon_warning, styles.icon)} />
          <Heading size="medium">{`Ønsker du å avbryte «${tiltaksgjennomforing?.navn}»?`}</Heading>
        </div>
      </Modal.Header>
      <Modal.Body className={styles.body}>
        {deltakerSummary && deltakerSummary.antallDeltakere > 0 && (
          <Alert variant="warning">
            {`Det finnes ${deltakerSummary.antallDeltakere} deltaker${deltakerSummary.antallDeltakere > 1 ? "e" : ""} på gjennomføringen. Ved å
           avbryte denne vil det føre til statusendring på alle deltakere som har en aktiv status.`}
          </Alert>
        )}
        <AvbrytModalAarsaker
          aarsak={aarsak}
          customAarsak={customAarsak}
          setAarsak={setAarsak}
          setCustomAarsak={setCustomAarsak}
          aarsakToString={
            <>
              {(
                Object.keys(
                  AvbrytGjennomforingRequest.aarsak,
                ) as Array<AvbrytGjennomforingRequest.aarsak>
              )
                .filter((a) => a !== AvbrytGjennomforingRequest.aarsak.AVBRUTT_I_ARENA)
                .map((a) => (
                  <Radio key={`${a}`} value={a}>
                    {aarsakToString(a)}
                  </Radio>
                ))}
            </>
          }
        />
        {mutation?.isError && (
          <AvbrytModalError aarsak={aarsak} customAarsak={customAarsak} mutation={mutation} />
        )}
      </Modal.Body>
      <Modal.Footer className={styles.footer}>
        <Button variant="secondary" type="button" onClick={onClose}>
          Nei, takk
        </Button>
        <Button variant="danger" onClick={handleAvbrytGjennomforing}>
          Ja, jeg vil avbryte gjennomføringen
        </Button>
      </Modal.Footer>
    </Modal>
  );
};
