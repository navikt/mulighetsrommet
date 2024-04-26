import { useAvbrytTiltaksgjennomforing } from "@/api/tiltaksgjennomforing/useAvbrytTiltaksgjennomforing";
import styles from "./AvbrytGjennomforingModal.module.scss";
import { resolveErrorMessage } from "@/api/errors";
import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import classNames from "classnames";
import { Heading, Button, Modal, RadioGroup, Radio, TextField, Alert } from "@navikt/ds-react";
import { AvbrytGjennomforingAarsak, Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { RefObject, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTiltaksgjennomforingDeltakerSummary } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingDeltakerSummary";

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
    if (mutation.isSuccess) {
      navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}`);
    }
  }, [mutation]);

  const handleAvbryt = () => {
    mutation.reset();
    mutation.mutate({
      id: tiltaksgjennomforing.id,
      aarsak: aarsak === "annet" ? customAarsak : aarsak,
    });
  };

  const aarsakToString = (aarsak: AvbrytGjennomforingAarsak): string => {
    switch (aarsak) {
      case AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA:
        return "Avbrutt i Arena";
      case AvbrytGjennomforingAarsak.BUDSJETT_HENSYN:
        return "Budsjetthensyn";
      case AvbrytGjennomforingAarsak.ENDRING_HOS_ARRANGOR:
        return "Endring hos arrangør";
      case AvbrytGjennomforingAarsak.FEILREGISTRERING:
        return "Feilregistrering";
      case AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE:
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
        <RadioGroup size="small" legend="Velg årsak." onChange={setAarsak} value={aarsak}>
          {(Object.keys(AvbrytGjennomforingAarsak) as Array<AvbrytGjennomforingAarsak>)
            .filter((a) => a !== AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA)
            .map((a) => (
              <Radio key={`${a}`} value={a}>
                {aarsakToString(a)}
              </Radio>
            ))}
          <Radio value="annet">
            Annet
            {aarsak === "annet" && (
              <TextField
                size="small"
                placeholder="beskrivelse"
                onChange={(e) => setCustomAarsak(e.target.value)}
                value={customAarsak ?? undefined}
                label={undefined}
              />
            )}
          </Radio>
        </RadioGroup>
        {mutation?.isError && (
          <div className={styles.error}>
            <b>
              •{" "}
              {aarsak === "annet" && !customAarsak
                ? "Beskrivelse er obligatorisk når “Annet” er valgt som årsak"
                : resolveErrorMessage(mutation.error)}
            </b>
          </div>
        )}
      </Modal.Body>
      <Modal.Footer className={styles.footer}>
        <Button variant="secondary" type="button" onClick={onClose}>
          Nei takk
        </Button>
        <Button variant="danger" onClick={handleAvbryt}>
          Ja, jeg vil avbryte gjennomføringen
        </Button>
      </Modal.Footer>
    </Modal>
  );
};
