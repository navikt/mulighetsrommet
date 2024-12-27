import { Alert, BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import { VeilederflateKontaktInfo, VeilederflateKontaktinfoTiltaksansvarlig } from "@mr/api-client";
import { RefObject, useRef } from "react";
import styles from "./KontaktinfoFane.module.scss";

const TEAMS_DYPLENKE = "https://teams.microsoft.com/l/chat/0/0?users=";

interface NavKontaktpersonInfoProps {
  kontaktinfo?: VeilederflateKontaktInfo;
}

const NavKontaktpersonInfo = ({ kontaktinfo }: NavKontaktpersonInfoProps) => {
  const modalRef = useRef<HTMLDialogElement>(null);

  if (!kontaktinfo) return null;

  const { tiltaksansvarlige } = kontaktinfo;

  return (
    <div className={styles.tiltaksansvarlig_info}>
      {tiltaksansvarlige.length === 0 ? (
        <Alert variant="info">Kontaktinfo til tiltaksansvarlig er ikke lagt inn</Alert>
      ) : (
        <>
          <Heading size="small" className={styles.header}>
            Tiltaksansvarlig
          </Heading>

          {tiltaksansvarlige.map((tiltaksansvarlig: VeilederflateKontaktinfoTiltaksansvarlig) => {
            const { navn, epost, telefon, enhet, beskrivelse } = tiltaksansvarlig;
            return (
              <div key={epost} className={styles.container}>
                <BodyShort className={styles.navn} size="small">
                  {navn}
                </BodyShort>
                {beskrivelse && (
                  <BodyShort textColor="subtle" size="small">
                    {beskrivelse}
                  </BodyShort>
                )}
                <BodyShort as="div" size="small">
                  <dl className={styles.definisjonsliste}>
                    <dt>Teams:</dt>
                    <dd>
                      <a
                        className={styles.teamslenke}
                        onClick={() => modalRef?.current?.showModal()}
                      >
                        Kontakt meg på Teams
                      </a>
                    </dd>
                    <dt>Epost:</dt>
                    <dd>
                      <a href={`mailto:${epost}`}>{epost}</a>
                    </dd>
                    {telefon ? (
                      <>
                        <dt>Telefon:</dt>
                        <dd>
                          <span>{telefon}</span>
                        </dd>
                      </>
                    ) : null}
                    {enhet ? (
                      <>
                        <dt>Enhet:</dt>
                        <dd>
                          <span>{`${enhet.navn} - ${enhet.enhetsnummer}`}</span>
                        </dd>
                      </>
                    ) : null}
                  </dl>
                </BodyShort>
                <PersonsensitiveOpplysningerModal modalRef={modalRef} epost={epost} />
              </div>
            );
          })}
        </>
      )}
    </div>
  );
};

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  epost: string;
}

function PersonsensitiveOpplysningerModal({ modalRef, epost }: Props) {
  function onClose() {
    modalRef?.current?.close();
  }

  function openTeams() {
    window.open(`${TEAMS_DYPLENKE}${encodeURIComponent(epost)}`, "_newtab");
    modalRef?.current?.close();
  }

  return (
    <Modal ref={modalRef} onClose={onClose} aria-label="modal">
      <Modal.Header closeButton>
        <div className={styles.heading}>
          <Heading size="medium">Personvern er viktig</Heading>
        </div>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>
          Ikke del personsensitive opplysninger når du diskuterer tiltak på Teams.
        </BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <div className={styles.knapperad}>
          <Button variant="secondary" onClick={openTeams}>
            Ok
          </Button>
        </div>
      </Modal.Footer>
    </Modal>
  );
}

export default NavKontaktpersonInfo;
