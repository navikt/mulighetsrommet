import {
  Alert,
  BodyLong,
  Button,
  GuidePanel,
  Heading,
  List,
  Modal,
  VStack,
} from "@navikt/ds-react";
import { ModalBody, ModalHeader } from "@navikt/ds-react/Modal";
import {
  PersonopplysningMedBeskrivelse,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { useState } from "react";
import { useBehandlingAvPersonopplysningerFraAvtale } from "../../api/queries/useBehandlingAvPersonopplysningerFraAvtale";
import { PersonvernIkon } from "../../ikoner/PersonvernIkon";
import styles from "./PersonvernContainer.module.scss";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

/**
 * Komponent som er ansvarlig for både rendering av GuidePanel og modal for personvern
 */
export function PersonvernContainer({ tiltaksgjennomforing }: Props) {
  const { data, isLoading, isError } = useBehandlingAvPersonopplysningerFraAvtale(
    tiltaksgjennomforing.avtaleId,
  );
  const [modalOpen, setModalOpen] = useState(false);

  if (isError) {
    return <Alert variant="error">Det skjedde en feil ved henting av personverninformasjon</Alert>;
  }

  if (!data || isLoading) {
    return null;
  }

  return (
    <>
      <GuidePanel illustration={<PersonvernIkon />} poster aria-label="Personvern">
        <VStack gap="5">
          <Heading level="4" size="small">
            Personvern og databehandling
          </Heading>
          <Button variant="tertiary" size="small" onClick={() => setModalOpen(true)}>
            Se hvilke personopplysninger du kan dele med tiltaksarrangøren for dette tiltaket
          </Button>
        </VStack>
      </GuidePanel>
      <Modal
        closeOnBackdropClick
        aria-label="Personvern og databehandling"
        open={modalOpen}
        onClose={() => setModalOpen(false)}
      >
        <ModalHeader>
          <VStack gap="5">
            <PersonvernIkon />
            <Heading level="2" size="medium">
              {tiltaksgjennomforing.navn}
            </Heading>
          </VStack>
        </ModalHeader>
        <ModalBody>
          <Heading level="3" size="small">
            {tiltaksgjennomforing.tiltakstype.navn}
          </Heading>
          <BodyLong spacing className={styles.lesebredde}>
            NAV har avtalt med tiltaksarrangøren at følgende opplysninger kan behandles om deltakere
            i tiltaket. Dersom du mener det er behov for å utveksle andre typer opplysninger om
            deltaker, må du ta kontakt med avtaleeier i tiltaksenheten/fylket.
          </BodyLong>
          <BodyLong spacing className={styles.lesebredde}>
            Det skal ikke deles flere personopplysninger enn det som står på denne listen. Husk også
            at selv om personopplysningene står på listen, må du gjøre en konkret vurdering i hvert
            enkelt tilfelle om det er nødvendig å dele opplysningene. Personopplysninger skal kun
            deles med tiltaksarrangør hvis det er nødvendig.
          </BodyLong>
          <VStack gap="5">
            <ListeOverPersonopplysninger
              title="Opplysninger om bruker som alltid kan/må behandles"
              personopplysninger={data.ALLTID ?? []}
            />
            <ListeOverPersonopplysninger
              title="Opplysninger om bruker som ofte er nødvendig og relevant å behandle"
              personopplysninger={data.OFTE ?? []}
            />
            <ListeOverPersonopplysninger
              title="Opplysninger om bruker som sjelden eller i helt spesielle tilfeller er nødvendig og relevant å behandle"
              personopplysninger={data.SJELDEN ?? []}
            />
          </VStack>
        </ModalBody>
      </Modal>
    </>
  );
}

interface ListeOverPersonopplysningerProps {
  title: string;
  personopplysninger: PersonopplysningMedBeskrivelse[];
}

function ListeOverPersonopplysninger({
  title,
  personopplysninger,
}: ListeOverPersonopplysningerProps) {
  if (personopplysninger.length === 0) {
    return null;
  }

  return (
    <List title={title} size="small">
      {personopplysninger.map((personopplysning) => (
        <List.Item key={personopplysning.personopplysning} className={styles.lesebredde}>
          {personopplysning.beskrivelse}
        </List.Item>
      ))}
    </List>
  );
}
