import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, Heading, Modal, Radio } from "@navikt/ds-react";
import classNames from "classnames";
import { AvbrytAvtaleAarsak, Avtale } from "mulighetsrommet-api-client";
import { RefObject, useEffect, useState } from "react";
import { useAvbrytAvtale } from "@/api/avtaler/useAvbrytAvtale";
import styles from "./AvbrytGjennomforingAvtaleModal.module.scss";
import { useNavigate } from "react-router-dom";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { AvbrytModalError } from "@/components/modal/AvbrytModalError";
import { AvbrytModalAarsaker } from "@/components/modal/AvbrytModalAarsaker";
import { useAktiveTiltaksgjennomforingerByAvtaleId } from "@/api/tiltaksgjennomforing/useAktiveTiltaksgjennomforingerByAvtaleId";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  avtale: Avtale;
}

export function AvbrytAvtaleModal({ modalRef, avtale }: Props) {
  const mutation = useAvbrytAvtale();
  const navigate = useNavigate();
  const [aarsak, setAarsak] = useState<string | null>(null);
  const [customAarsak, setCustomAarsak] = useState<string | null>(null);

  const { data: tiltaksgjennomforingerMedAvtaleId } = useAktiveTiltaksgjennomforingerByAvtaleId(
    avtale.id,
  );

  const avtalenHarGjennomforinger =
    tiltaksgjennomforingerMedAvtaleId && tiltaksgjennomforingerMedAvtaleId.data.length > 0;

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

  const aarsakToString = (aarsak: AvbrytAvtaleAarsak): string => {
    switch (aarsak) {
      case AvbrytAvtaleAarsak.AVBRUTT_I_ARENA:
        return "Avbrutt i Arena";
      case AvbrytAvtaleAarsak.BUDSJETT_HENSYN:
        return "Budsjetthensyn";
      case AvbrytAvtaleAarsak.ENDRING_HOS_ARRANGOR:
        return "Endring hos arrangør";
      case AvbrytAvtaleAarsak.FEILREGISTRERING:
        return "Feilregistrering";
    }
  };

  function pluralGjennomforingTekst(antall: number, tekst: string) {
    return tiltaksgjennomforingerMedAvtaleId &&
      tiltaksgjennomforingerMedAvtaleId.data.length > antall
      ? tekst
      : "";
  }

  return (
    <Modal ref={modalRef} onClose={onClose} closeOnBackdropClick aria-label="modal">
      <Modal.Header closeButton={false}>
        <div className={styles.heading}>
          <XMarkOctagonFillIcon className={classNames(styles.icon_warning, styles.icon)} />
          <Heading size="medium">
            {mutation.isError || avtalenHarGjennomforinger
              ? `Kan ikke avbryte «${avtale?.navn}»`
              : `Ønsker du å avbryte «${avtale?.navn}»?`}
          </Heading>
        </div>
      </Modal.Header>
      <Modal.Body className={styles.body}>
        {avtalenHarGjennomforinger ? (
          <Alert variant="warning">
            {`Avtaler med aktive gjennomføringer kan ikke avbrytes. Det er 
            ${tiltaksgjennomforingerMedAvtaleId.data.length} 
            aktiv${pluralGjennomforingTekst(1, "e")} 
            gjennomføring${pluralGjennomforingTekst(1, "er")} 
            under denne avtalen. Vurder om du vil avbryte 
            gjennomføringen${pluralGjennomforingTekst(0, "e")}.`}
          </Alert>
        ) : (
          <AvbrytModalAarsaker
            aarsak={aarsak}
            customAarsak={customAarsak}
            setAarsak={setAarsak}
            setCustomAarsak={setCustomAarsak}
            radioknapp={
              <>
                {(Object.keys(AvbrytAvtaleAarsak) as Array<AvbrytAvtaleAarsak>)
                  .filter((a) => a !== AvbrytAvtaleAarsak.AVBRUTT_I_ARENA)
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
      {avtalenHarGjennomforinger ? (
        <Modal.Footer>
          <Button onClick={onClose}>Ok</Button>
        </Modal.Footer>
      ) : (
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
      )}
    </Modal>
  );
}
