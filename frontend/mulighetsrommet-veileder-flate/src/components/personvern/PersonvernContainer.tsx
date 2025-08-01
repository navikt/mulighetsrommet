import {
  BodyLong,
  Button,
  GuidePanel,
  Heading,
  HelpText,
  HStack,
  List,
  Modal,
  VStack,
} from "@navikt/ds-react";
import { ModalBody, ModalHeader } from "@navikt/ds-react/Modal";
import { PersonopplysningData, VeilederflateTiltakGruppe } from "@api-client";
import { useState } from "react";
import { PersonvernIkon } from "@/ikoner/PersonvernIkon";

interface Props {
  tiltak: VeilederflateTiltakGruppe;
}

/**
 * Komponent som er ansvarlig for både rendering av GuidePanel og modal for personvern
 */
export function PersonvernContainer({ tiltak }: Props) {
  const [modalOpen, setModalOpen] = useState(false);

  return (
    <>
      <GuidePanel
        illustration={<PersonvernIkon aria-label="Ikon som illustrerer personvern" />}
        aria-label="Personvern"
      >
        <VStack gap="5">
          <Heading level="4" size="small">
            Personvern og databehandling
          </Heading>
          <Button
            variant="tertiary"
            size="small"
            onClick={() => setModalOpen(true)}
            className="text-left p-0 m-0 underline [&_.navds-label]:font-normal"
          >
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
            <PersonvernIkon aria-label="Ikon som illustrerer personvern" />
            <Heading level="2" size="medium">
              {tiltak.tiltakstype.navn}
            </Heading>
          </VStack>
        </ModalHeader>
        <ModalBody>
          <BodyLong spacing className="max-w-[75ch]">
            Nav har avtalt med tiltaksarrangøren at følgende opplysninger kan behandles om deltakere
            i tiltaket. Dersom du mener det er behov for å utveksle andre typer opplysninger om
            deltaker, må du ta kontakt med avtaleeier i tiltaksenheten/fylket.
          </BodyLong>
          <BodyLong spacing className="max-w-[75ch]">
            Det skal ikke deles flere personopplysninger enn det som står på denne listen. Selv om
            personopplysningene står på listen, må du gjøre en konkret vurdering i hvert enkelt
            tilfelle om det er nødvendig å dele opplysningene. Husk prinsippet om dataminimering, og
            at vi ikke skal sende flere opplysninger enn det som er nødvendig.
          </BodyLong>
          <BodyLong as="div" spacing className="max-w-[75ch]">
            <div className="flex items-baseline gap-2">
              <p className="m-0">
                Personopplysninger om deltakers nærstående skal i utgangspunktet ikke behandles. I
                enkelte tilfeller kan det likevel være nødvendig å behandle indirekte opplysninger
                om deltakers nærstående, fordi det kan ha betydning for tiltaket.
              </p>
              <HelpText>
                Dataminimeringsprinsippet gjelder også her: man kan for eksempel opplyse om at
                deltaker har et nært familiemedlem med stort omsorgsbehov, uten å opplyse om
                vedkommendes relasjon til deltaker, diagnose, navn og alder.{" "}
              </HelpText>
            </div>
          </BodyLong>
          <VStack gap="5">
            <ListeOverPersonopplysninger
              title="Opplysninger om deltaker som kan behandles"
              personopplysninger={tiltak.personopplysningerSomKanBehandles}
            />
          </VStack>
        </ModalBody>
      </Modal>
    </>
  );
}

interface ListeOverPersonopplysningerProps {
  title: string;
  personopplysninger: PersonopplysningData[];
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
        <List.Item key={personopplysning.personopplysning} className="max-w-[75ch]">
          <HStack align={"end"} gap="1">
            <div className="flex items-baseline gap-2">
              {personopplysning.tittel}{" "}
              {personopplysning.hjelpetekst ? (
                <HelpText>{personopplysning.hjelpetekst}</HelpText>
              ) : null}
            </div>
          </HStack>
        </List.Item>
      ))}
    </List>
  );
}
