import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, Heading, Modal, Radio } from "@navikt/ds-react";
import classNames from "classnames";
import { AvbrytAvtaleRequest, Avtale } from "mulighetsrommet-api-client";
import { RefObject, useEffect, useState } from "react";
import { useAvbrytAvtale } from "@/api/avtaler/useAvbrytAvtale";
import styles from "./AvbrytGjennomforingAvtaleModal.module.scss";
import { useNavigate } from "react-router-dom";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { AvbrytModalError } from "@/components/modal/AvbrytModalError";
import { AvbrytModalAarsaker } from "@/components/modal/AvbrytModalAarsaker";
import { useTiltaksgjennomforingerByAvtaleId } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingerByAvtaleId";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  avtale: Avtale;
}

export function AvbrytAvtaleModal({ modalRef, avtale }: Props) {
  const mutation = useAvbrytAvtale();
  const navigate = useNavigate();

  const [aarsak, setAarsak] = useState<string | null>(null);
  const [customAarsak, setCustomAarsak] = useState<string | null>(null);

  const { data: tiltaksgjennomforingerMedAvtaleId } = useTiltaksgjennomforingerByAvtaleId(
    avtale.id,
  );

  const onClose = () => {
    mutation.reset();
    modalRef.current?.close();
  };

  useEffect(() => {
    modalRef.current?.close();
    navigate(`/avtaler/${avtale.id}`);
  }, [mutation.isSuccess]);

  const handleAvbrytAvtale = () => {
    mutation.reset();
    if (avtale?.id) {
      mutation.mutate({
        id: avtale?.id,
        aarsak: aarsak === "annet" ? customAarsak : aarsak,
      });
    }
  };

  const aarsakToString = (aarsak: AvbrytAvtaleRequest.aarsak): string => {
    switch (aarsak) {
      case AvbrytAvtaleRequest.aarsak.AVBRUTT_I_ARENA:
        return "Avbrutt i Arena";
      case AvbrytAvtaleRequest.aarsak.BUDSJETT_HENSYN:
        return "Budsjetthensyn";
      case AvbrytAvtaleRequest.aarsak.ENDRING_HOS_ARRANGOR:
        return "Endring hos arrangør";
      case AvbrytAvtaleRequest.aarsak.FEILREGISTRERING:
        return "Feilregistrering";
    }
  };

  return (
    <Modal ref={modalRef} onClose={onClose} closeOnBackdropClick aria-label="modal">
      <Modal.Header closeButton={false}>
        <div className={styles.heading}>
          <XMarkOctagonFillIcon className={classNames(styles.icon_warning, styles.icon)} />
          <Heading size="medium">
            {mutation.isError
              ? `Kan ikke avbryte «${avtale?.navn}»`
              : `Ønsker du å avbryte «${avtale?.navn}»?`}
          </Heading>
        </div>
      </Modal.Header>
      <Modal.Body className={styles.body}>
        {tiltaksgjennomforingerMedAvtaleId && tiltaksgjennomforingerMedAvtaleId.data.length > 0 ? (
          <Alert variant="warning">
            {`Avtaler med aktive gjennomføringer kan ikke avbrytes. Det er 
            ${tiltaksgjennomforingerMedAvtaleId.data.length} 
            aktiv${tiltaksgjennomforingerMedAvtaleId.data.length > 1 ? "e" : ""} 
            gjennomføring${tiltaksgjennomforingerMedAvtaleId.data.length > 1 ? "er" : ""} 
            under denne avtalen. Vurder om du vil avbryte 
            gjennomføringen${tiltaksgjennomforingerMedAvtaleId.data.length > 1 ? "e" : ""}.`}
          </Alert>
        ) : (
          <AvbrytModalAarsaker
            aarsak={aarsak}
            customAarsak={customAarsak}
            setAarsak={setAarsak}
            setCustomAarsak={setCustomAarsak}
            aarsakToString={
              <>
                {(Object.keys(AvbrytAvtaleRequest.aarsak) as Array<AvbrytAvtaleRequest.aarsak>)
                  .filter((a) => a !== AvbrytAvtaleRequest.aarsak.AVBRUTT_I_ARENA)
                  .map((a) => (
                    <Radio key={`${a}`} value={a}>
                      {aarsakToString(a)}
                    </Radio>
                  ))}
              </>
            }
          />
        )}
        <BodyShort>
          {mutation?.isError && (
            <AvbrytModalError aarsak={aarsak} customAarsak={customAarsak} mutation={mutation} />
          )}
        </BodyShort>
      </Modal.Body>
      {tiltaksgjennomforingerMedAvtaleId && tiltaksgjennomforingerMedAvtaleId.data.length === 0 ? (
        <Modal.Footer className={styles.footer}>
          <Button variant="secondary" onClick={onClose}>
            Nei, takk
          </Button>
          <HarSkrivetilgang ressurs="Avtale">
            <Button variant="danger" onClick={handleAvbrytAvtale}>
              Ja, jeg vil avbryte avtalen
            </Button>
          </HarSkrivetilgang>
        </Modal.Footer>
      ) : (
        <Modal.Footer>
          <Button onClick={onClose}>OK</Button>
        </Modal.Footer>
      )}
    </Modal>
  );
}
