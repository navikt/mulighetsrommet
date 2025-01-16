import { useDeleteArrangorKontaktperson } from "@/api/arrangor/useDeleteArrangorKontaktperson";
import { useKoblingerTilDokumenterForKontaktpersonHosArrangor } from "@/api/arrangor/useKoblingerTilDokumenterForKontaktpersonHosArrangor";
import { useFrikobleArrangorKontaktpersonFraAvtale } from "@/api/avtaler/useFrikobleArrangorKontaktpersonFraAvtale";
import { useFrikobleArrangorKontaktpersonFraGjennomforing } from "@/api/gjennomforing/useFrikobleArrangorKontaktpersonFraGjennomforing";
import { ArrangorKontaktperson, DokumentKoblingForKontaktperson } from "@mr/api-client";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { Alert, BodyShort, Button, Heading, Table, VStack } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import { RefObject } from "react";
import { Laster } from "../laster/Laster";
import styles from "./SlettKontaktpersonModal.module.scss";

interface Props {
  onClose: () => void;
  kontaktperson: ArrangorKontaktperson;
  modalRef: RefObject<HTMLDialogElement>;
}

export function SlettKontaktpersonModal({ onClose, kontaktperson, modalRef }: Props) {
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
    <VarselModal
      modalRef={modalRef}
      open={!!kontaktperson}
      handleClose={onClose}
      headingText="Slett kontaktperson"
      headingIconType="error"
      body={
        !data || isLoading ? (
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
            ) : null}
            <BodyShort>Er du sikker på at du vil slette kontaktpersonen?</BodyShort>
          </>
        )
      }
      secondaryButton
      primaryButton={
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
          Ja, jeg vil slette kontaktpersonen
        </Button>
      }
    />
  );
}

interface KoblingsoversiktProps {
  avtaler: DokumentKoblingForKontaktperson[];
  gjennomforinger: DokumentKoblingForKontaktperson[];
  kontaktperson: ArrangorKontaktperson;
}

function Koblingsoversikt({ avtaler, gjennomforinger, kontaktperson }: KoblingsoversiktProps) {
  const frikobleFraAvtaleMutation = useFrikobleArrangorKontaktpersonFraAvtale();
  const frikobleFraGjennomforingMutation = useFrikobleArrangorKontaktpersonFraGjennomforing();
  return (
    <div>
      <p>{kontaktperson.navn} er koblet til følgende og må fjernes før hen kan slettes.</p>
      <VStack gap="5">
        <DokumentKoblinger
          baseUrl="avtaler"
          dokumenter={avtaler}
          kontaktperson={kontaktperson}
          frikobleMutation={frikobleFraAvtaleMutation}
        />
        <DokumentKoblinger
          baseUrl="tiltaksgjennomforinger"
          dokumenter={gjennomforinger}
          kontaktperson={kontaktperson}
          frikobleMutation={frikobleFraGjennomforingMutation}
        />
      </VStack>
    </div>
  );
}

interface DokumentKoblingerProps {
  dokumenter: DokumentKoblingForKontaktperson[];
  kontaktperson: ArrangorKontaktperson;
  baseUrl: "tiltaksgjennomforinger" | "avtaler";
  frikobleMutation: UseMutationResult<
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
  frikobleMutation,
}: DokumentKoblingerProps) {
  return (
    <div>
      <Heading level="2" size="small">
        {baseUrl === "avtaler"
          ? `Avtaler (${dokumenter.length})`
          : `Gjennomføringer (${dokumenter.length})`}
      </Heading>
      {frikobleMutation.error ? (
        <Alert variant="warning">Klarte ikke fjerne kontaktperson</Alert>
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
              <Table.HeaderCell>Navn</Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body className={styles.tableBody}>
            {dokumenter.map((dokument) => (
              <Table.Row key={dokument.id}>
                <Table.DataCell className={styles.name_column}>
                  <Lenke to={`/${baseUrl}/${dokument.id}/skjema`} isExternal>
                    {dokument.navn}
                  </Lenke>
                </Table.DataCell>
                <Table.DataCell>
                  <Button
                    size="small"
                    variant="danger"
                    onClick={() =>
                      frikobleMutation.mutate({
                        kontaktpersonId: kontaktperson.id,
                        dokumentId: dokument.id,
                      })
                    }
                  >
                    Fjern
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
