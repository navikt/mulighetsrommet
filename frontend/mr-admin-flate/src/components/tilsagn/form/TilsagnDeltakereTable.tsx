import { DataElementStatusTag } from "@mr/frontend-common";
import { CheckmarkCircleIcon } from "@navikt/aksel-icons";
import { BodyShort, Table, VStack } from "@navikt/ds-react";
import { TilsagnDeltakerDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  deltakere: TilsagnDeltakerDto[];
  selected?: (a: TilsagnDeltakerDto) => boolean;
  onClick?: (a: TilsagnDeltakerDto) => void;
}

export function TilsagnDeltakereTable({ deltakere, selected, onClick }: Props) {
  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Deltaker</Table.HeaderCell>
          <Table.HeaderCell scope="col">Innhold</Table.HeaderCell>
          <Table.HeaderCell scope="col">Oppfølgingsenhet</Table.HeaderCell>
          <Table.HeaderCell scope="col">Status</Table.HeaderCell>
          <Table.HeaderCell scope="col"></Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {deltakere.map((deltaker, i) => {
          return (
            <Table.Row
              key={i + deltaker.deltakerId}
              selected={selected?.(deltaker)}
              onClick={() => onClick?.(deltaker)}
            >
              <Table.HeaderCell scope="row">
                <VStack>
                  <BodyShort className="font-bold">{deltaker.navn}</BodyShort>
                  <BodyShort>{deltaker.norskIdent}</BodyShort>
                </VStack>
              </Table.HeaderCell>
              <Table.DataCell>{deltaker.innholdAnnet}</Table.DataCell>
              <Table.DataCell>{deltaker.oppfolgingEnhet?.navn ?? "-"}</Table.DataCell>
              <Table.DataCell>
                <DataElementStatusTag {...deltaker.status} />
              </Table.DataCell>
              <Table.DataCell>
                {selected?.(deltaker) && (
                  <CheckmarkCircleIcon
                    color="var(--ax-text-success-decoration)"
                    className="text-ax-success-strong"
                    title="a11y-title"
                    fontSize="2rem"
                  />
                )}
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
