import {
  VeilederflateKontaktinfo,
  VeilederflateKontaktinfoTiltaksansvarlig,
} from "@arbeidsmarkedstiltak/api-client";
import { Alert, BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import { RefObject, useRef } from "react";
import { KontaktpersonBox } from "./KontaktpersonBox";

const TEAMS_DYPLENKE = "https://teams.microsoft.com/l/chat/0/0?users=";

interface NavKontaktpersonInfoProps {
  kontaktinfo?: VeilederflateKontaktinfo;
}

export function NavKontaktpersonInfo({ kontaktinfo }: NavKontaktpersonInfoProps) {
  const modalRef = useRef<HTMLDialogElement>(null);

  if (!kontaktinfo) return null;

  const { tiltaksansvarlige } = kontaktinfo;

  return (
    <div>
      {tiltaksansvarlige.length === 0 ? (
        <Alert variant="info">Kontaktinfo til tiltaksansvarlig er ikke lagt inn</Alert>
      ) : (
        <>
          <Heading size="small" className="pb-5">
            {tiltaksansvarlige.length > 1 ? "Tiltaksansvarlige" : "Tiltaksansvarlig"}
          </Heading>

          {tiltaksansvarlige.map((tiltaksansvarlig: VeilederflateKontaktinfoTiltaksansvarlig) => {
            const { navn, epost, telefon, enhet, beskrivelse } = tiltaksansvarlig;
            return (
              <KontaktpersonBox key={epost}>
                <Heading level="4" size="xsmall" className="font-bold">
                  {navn}
                </Heading>
                {beskrivelse && (
                  <BodyShort textColor="subtle" size="small">
                    {beskrivelse}
                  </BodyShort>
                )}
                <BodyShort as="div" size="small">
                  <dl className="flex flex-col gap-1">
                    <div>
                      <dt className="inline">Teams:</dt>
                      <dd className="inline">
                        <Button
                          as="a"
                          variant="tertiary"
                          size="small"
                          className="inline"
                          onClick={() => modalRef.current?.showModal()}
                        >
                          Kontakt meg på Teams
                        </Button>
                      </dd>
                    </div>
                    <div>
                      <dt className="inline">Epost:</dt>
                      <dd className="inline">
                        <a href={`mailto:${epost}`}>{epost}</a>
                      </dd>
                    </div>
                    {telefon ? (
                      <div>
                        <dt className="inline">Telefon:</dt>
                        <dd className="inline">
                          <span>{telefon}</span>
                        </dd>
                      </div>
                    ) : null}
                    {enhet ? (
                      <div>
                        <dt className="inline">Enhet:</dt>
                        <dd className="inline">
                          <span>{`${enhet.navn} - ${enhet.enhetsnummer}`}</span>
                        </dd>
                      </div>
                    ) : null}
                  </dl>
                </BodyShort>
                {epost && <PersonsensitiveOpplysningerModal modalRef={modalRef} epost={epost} />}
              </KontaktpersonBox>
            );
          })}
        </>
      )}
    </div>
  );
}

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  epost: string;
}

function PersonsensitiveOpplysningerModal({ modalRef, epost }: Props) {
  function onClose() {
    modalRef.current?.close();
  }

  function openTeams() {
    window.open(`${TEAMS_DYPLENKE}${encodeURIComponent(epost)}`, "_newtab");
    modalRef.current?.close();
  }

  return (
    <Modal ref={modalRef} onClose={onClose} aria-label="modal">
      <Modal.Header closeButton>
        <div>
          <Heading size="medium">Personvern er viktig</Heading>
        </div>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>
          Ikke del personsensitive opplysninger når du diskuterer tiltak på Teams.
        </BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <div>
          <Button variant="secondary" onClick={openTeams}>
            Ok
          </Button>
        </div>
      </Modal.Footer>
    </Modal>
  );
}
