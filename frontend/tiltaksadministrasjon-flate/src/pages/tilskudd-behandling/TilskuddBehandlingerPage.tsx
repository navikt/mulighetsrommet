import { useTilskuddBehandlinger } from "@/api/tilskudd-behandling/useTilskuddBehandlinger";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { DataElementStatusTag, useSortableData } from "@mr/frontend-common";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { Alert, Table, Tag } from "@navikt/ds-react";
import {
  formaterDato,
  formaterPeriodeSlutt,
  formaterPeriodeStart,
} from "@mr/frontend-common/utils/date";
import { opplaeringTilskuddToString } from "@/utils/Utils";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { SamletVedtakResultat, TilskuddBehandlingKompakt } from "@tiltaksadministrasjon/api-client";
import { GavelSoundBlockIcon, PiggybankIcon } from "@navikt/aksel-icons";

export function TilskuddBehandlingerPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: behandlinger } = useTilskuddBehandlinger(gjennomforingId);
  const { sortedData, sort, toggleSort } = useSortableData(behandlinger, undefined, (item, key) => {
    if (key === "tilskuddtyper") {
      return item[key].sort().at(0);
    } else {
      return key.split(".").reduce((obj: any, k) => obj?.[k], item);
    }
  });

  return (
    <>
      <KnapperadContainer>
        <Handlinger
          grupper={[
            {
              items: [
                {
                  label: "Opprett tilskuddsbehandling",
                  href: "opprett",
                },
              ],
            },
          ]}
        />
      </KnapperadContainer>
      {behandlinger.length === 0 && (
        <Alert variant="info" className="mt-4">
          Det finnes ingen tilskuddsbehandlinger for dette tiltaket
        </Alert>
      )}
      {sortedData.length > 0 && (
        <Table sort={sort} onSortChange={(sortKey) => toggleSort(sortKey as string)}>
          <Table.Header>
            <Table.Row>
              <TableColumnHeader sortKey="soknadDato" sortable>
                Innsendt
              </TableColumnHeader>
              <TableColumnHeader sortKey="journalpostId" sortable>
                Journalpost-ID
              </TableColumnHeader>
              <TableColumnHeader sortKey="periode.start" sortable>
                Periodestart
              </TableColumnHeader>
              <TableColumnHeader sortKey="periode.slutt" sortable>
                Periodeslutt
              </TableColumnHeader>
              <TableColumnHeader sortKey="tilskuddtyper" sortable>
                Tilskuddstype
              </TableColumnHeader>
              <TableColumnHeader sortKey="tilskuddtyper" sortable>
                Vedtaksresultat
              </TableColumnHeader>
              <TableColumnHeader sortKey="status.type" sortable>
                Behandlingsstatus
              </TableColumnHeader>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {sortedData.map((b: TilskuddBehandlingKompakt) => (
              <Table.Row key={b.id}>
                <Table.DataCell>{formaterDato(b.soknadDato)}</Table.DataCell>
                <Table.DataCell>{b.journalpostId}</Table.DataCell>
                <Table.DataCell>{formaterPeriodeStart(b.periode)}</Table.DataCell>
                <Table.DataCell>{formaterPeriodeSlutt(b.periode)}</Table.DataCell>
                <Table.DataCell>
                  {b.tilskuddtyper.map((t) => opplaeringTilskuddToString(t)).join(", ")}
                </Table.DataCell>
                <Table.DataCell>
                  <SamletVedtakResultatStatusTag status={b.samletVedtakResultat} />
                </Table.DataCell>
                <Table.DataCell>
                  <DataElementStatusTag {...b.status.status} />
                </Table.DataCell>
                <Table.DataCell>
                  <Lenke to={b.id}> Detaljer </Lenke>
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </>
  );
}

function SamletVedtakResultatStatusTag({ status }: { status: SamletVedtakResultat }) {
  switch (status) {
    case SamletVedtakResultat.INNVILGELSE:
      return (
        <Tag size="small" data-color="success" icon={<PiggybankIcon fontSize="1rem" />}>
          Innvilgelse
        </Tag>
      );
    case SamletVedtakResultat.DELVIS_INNVILGELSE:
      return (
        <Tag size="small" data-color="warning" icon={<GavelSoundBlockIcon fontSize="1rem" />}>
          Delvis innvilgelse
        </Tag>
      );
    case SamletVedtakResultat.AVSLAG:
      return (
        <Tag size="small" data-color="danger" icon={<PiggybankIcon fontSize="1rem" />}>
          Avslag
        </Tag>
      );
  }
}
