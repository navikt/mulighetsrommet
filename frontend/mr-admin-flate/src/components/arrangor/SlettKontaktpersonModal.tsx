import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, HStack, Heading, Modal, Table, VStack } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import { ArrangorKontaktperson, DokumentKoblingForKontaktperson } from "mulighetsrommet-api-client";
import { Link } from "react-router-dom";
import { useDeleteArrangorKontaktperson } from "../../api/arrangor/useDeleteArrangorKontaktperson";
import { useKoblingerTilDokumenterForKontaktpersonHosArrangor } from "../../api/arrangor/useKoblingerTilDokumenterForKontaktpersonHosArrangor";
import { useFrikobleArrangorKontaktpersonFraAvtale } from "../../api/avtaler/useFrikobleArrangorKontaktpersonFraAvtale";
import { useFrikobleArrangorKontaktpersonFraTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useFrikobleArrangorKontaktpersonFraTiltaksgjennomforing";
import { Laster } from "../laster/Laster";

interface Props {
  onClose: () => void;
  kontaktperson: ArrangorKontaktperson;
}

export function SlettKontaktpersonModal({ onClose, kontaktperson }: Props) {
  const { data, isLoading } = useKoblingerTilDokumenterForKontaktpersonHosArrangor(
    kontaktperson.id,
  );
  const deleteArrangorKontaktpersonMutation = useDeleteArrangorKontaktperson();

  const { avtaler, gjennomforinger } = data || { avtaler: [], gjennomforinger: [] };
  const erKobletTilDokumenter = avtaler.length + gjennomforinger.length > 0;

  function slettKontaktperson() {
    deleteArrangorKontaktpersonMutation.mutate(
      {
        arrangorId: kontaktperson.arrangorId,
        kontaktpersonId: kontaktperson.id,
      },
      {
        onSuccess: onClose,
      },
    );
  }

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
            {deleteArrangorKontaktpersonMutation.error ? (
              <Alert variant="warning">Klarte ikke slette kontaktperson</Alert>
            ) : erKobletTilDokumenter ? (
              <Koblingsoversikt
                kontaktperson={kontaktperson}
                avtaler={avtaler}
                gjennomforinger={gjennomforinger}
              />
            ) : (
              <Alert variant="success" size="small">
                {kontaktperson.navn} er ikke kontaktperson for noen avtaler eller gjennomføringer
              </Alert>
            )}
            <p>Er du sikker på at du vil slette kontaktpersonen?</p>
            <BodyShort>
              <i>Dette kan ikke angres.</i>
            </BodyShort>
            <HStack justify={"space-between"} style={{ marginTop: "1rem" }}>
              <Button
                title={
                  erKobletTilDokumenter
                    ? "Du må fjerne kontaktpersonen fra avtaler og/eller gjennomføringer før du kan slette hen"
                    : ""
                }
                disabled={erKobletTilDokumenter}
                variant="danger"
                onClick={slettKontaktperson}
              >
                Slett kontaktperson
              </Button>
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
  const frikobleFraAvtaleMutation = useFrikobleArrangorKontaktpersonFraAvtale();
  const frikobleFraGjennomforingMutation =
    useFrikobleArrangorKontaktpersonFraTiltaksgjennomforing();
  return (
    <div>
      <p>{kontaktperson.navn} er koblet til følgende dokumenter:</p>
      <VStack gap="5">
        <DokumentKoblinger
          baseUrl="avtaler"
          dokumenter={avtaler}
          kontaktperson={kontaktperson}
          fristillMutation={frikobleFraAvtaleMutation}
        />
        <DokumentKoblinger
          baseUrl="tiltaksgjennomforinger"
          dokumenter={gjennomforinger}
          kontaktperson={kontaktperson}
          fristillMutation={frikobleFraGjennomforingMutation}
        />
      </VStack>
    </div>
  );
}

interface DokumentKoblingerProps {
  dokumenter: DokumentKoblingForKontaktperson[];
  kontaktperson: ArrangorKontaktperson;
  baseUrl: "tiltaksgjennomforinger" | "avtaler";
  fristillMutation: UseMutationResult<
    string,
    Error,
    {
      kontaktpersonId: string;
      dokumentId: string;
    },
    unknown
  >;
}

function DokumentKoblinger({
  dokumenter,
  kontaktperson,
  baseUrl,
  fristillMutation,
}: DokumentKoblingerProps) {
  return (
    <div>
      <Heading level="2" size="small">
        {baseUrl === "avtaler" ? "Avtaler" : "Gjennomføringer"}
      </Heading>
      {fristillMutation.error ? (
        <Alert variant="warning">Klarte ikke fristille kontaktperson</Alert>
      ) : null}
      {dokumenter.length === 0 ? (
        <Alert variant="success" inline size="small">
          {kontaktperson.navn} er ikke kontaktperson for noen{" "}
          {baseUrl === "avtaler" ? "avtaler" : "gjennomføringer"}
        </Alert>
      ) : (
        <Table zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>
                {baseUrl === "avtaler" ? "Avtalenavn" : "Gjennomføringsnavn"}
              </Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {dokumenter.map((dokument) => (
              <Table.Row key={dokument.id}>
                <Table.DataCell>
                  <Link
                    target="_blank"
                    rel="noopener noreferrer"
                    to={`/${baseUrl}/${dokument.id}/skjema`}
                  >
                    {dokument.navn} <ExternalLinkIcon />
                  </Link>
                </Table.DataCell>
                <Table.DataCell>
                  <Button
                    size="small"
                    variant="danger"
                    onClick={() =>
                      fristillMutation.mutate({
                        kontaktpersonId: kontaktperson.id,
                        dokumentId: dokument.id,
                      })
                    }
                  >
                    Fjern fra {baseUrl === "avtaler" ? "avtale" : "gjennomføring"}
                  </Button>
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </div>
  );
}
