import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, HStack, Heading, Modal } from "@navikt/ds-react";
import { ArrangorKontaktperson, DokumentKoblingForKontaktperson } from "mulighetsrommet-api-client";
import { Link } from "react-router-dom";
import { useKoblingerTilDokumenterForKontaktpersonHosArrangor } from "../../api/arrangor/useKoblingerTilDokumenterForKontaktpersonHosArrangor";
import { Laster } from "../laster/Laster";

interface Props {
  onClose: () => void;
  kontaktperson: ArrangorKontaktperson;
}

export function SlettKontaktpersonModal({ onClose, kontaktperson }: Props) {
  const { data, isLoading } = useKoblingerTilDokumenterForKontaktpersonHosArrangor(
    kontaktperson.id,
  );
  // TODO Hent koblinger til dokumenter for kontaktpersonen
  // Vis dem i liste for gjennomføringer og avtaler med mulighet for å fristille automatisk
  // (Må lage endepunkt for å fristille dokumenter fra kontaktpersoner)

  const { avtaler, gjennomforinger } = data || { avtaler: [], gjennomforinger: [] };
  const erKobletTilDokumenter = avtaler.length + gjennomforinger.length > 0;

  // useDeleteArrangorKontaktperson();
  return (
    <Modal
      open={!!kontaktperson}
      onClose={onClose}
      aria-label="Slettemodal"
      width="50rem"
      closeOnBackdropClick
    >
      <Modal.Header closeButton>
        <Heading size="medium">Slett kontaktperson</Heading>
      </Modal.Header>

      <Modal.Body>
        {!data || isLoading ? (
          <Laster tekst="Henter koblinger til dokumenter..." />
        ) : (
          <>
            {erKobletTilDokumenter ? (
              <Koblingsoversikt
                kontaktperson={kontaktperson}
                avtaler={avtaler}
                gjennomforinger={gjennomforinger}
              />
            ) : null}
            <BodyShort>Er du sikker på at du vil slette kontaktpersonen?</BodyShort>
            <BodyShort>
              <i>Dette kan ikke angres.</i>
            </BodyShort>
            <HStack justify={"space-between"} style={{ marginTop: "1rem" }}>
              <Button variant="danger">Slett kontaktperson</Button>
              <Button variant="tertiary" onClick={onClose}>
                Nei, avbryt
              </Button>
            </HStack>
          </>
        )}
      </Modal.Body>
    </Modal>
  );
}

interface KoblingsoversiktProps {
  avtaler: DokumentKoblingForKontaktperson[];
  gjennomforinger: DokumentKoblingForKontaktperson[];
  kontaktperson: ArrangorKontaktperson;
}

function Koblingsoversikt({ avtaler, gjennomforinger, kontaktperson }: KoblingsoversiktProps) {
  return (
    <div>
      <p>{kontaktperson.navn} er koblet til følgende dokumenter:</p>
      <Heading level="2" size="small">
        Avtaler
      </Heading>
      {avtaler.length === 0 ? (
        <Alert variant="success" inline size="small">
          {kontaktperson.navn} er ikke kontaktperson for noen avtaler
        </Alert>
      ) : (
        <ul>
          {avtaler.map((avtale) => (
            <li key={avtale.id}>
              <Link target="_blank" rel="noopener noreferrer" to={`/avtaler/${avtale.id}/skjema`}>
                {avtale.navn} <ExternalLinkIcon />
              </Link>
            </li>
          ))}
        </ul>
      )}

      <Heading level="2" size="small">
        Gjennomføringer
      </Heading>
      {gjennomforinger.length === 0 ? (
        <Alert variant="success" inline size="small">
          {kontaktperson.navn} er ikke kontaktperson for noen gjennomføringer
        </Alert>
      ) : (
        <ul>
          {gjennomforinger.map((gjennomforing) => (
            <li key={gjennomforing.id}>
              <Link
                target="_blank"
                rel="noopener noreferrer"
                to={`/tiltaksgjennomforinger/${gjennomforing.id}/skjema`}
              >
                {gjennomforing.navn} <ExternalLinkIcon />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
