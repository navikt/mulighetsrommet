import { NavnOgGradering } from "@/components/personalia/NavnOgGradering";
import { DataElementStatusTag } from "@mr/frontend-common";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { CheckmarkCircleIcon } from "@navikt/aksel-icons";
import { BodyShort, Table, Tooltip, VStack } from "@navikt/ds-react";
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
          <Table.HeaderCell scope="col">Oppfølgingsenhet</Table.HeaderCell>
          <Table.HeaderCell scope="col">Innhold</Table.HeaderCell>
          <Table.HeaderCell scope="col">Start</Table.HeaderCell>
          <Table.HeaderCell scope="col">Slutt</Table.HeaderCell>
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
                  <NavnOgGradering navn={deltaker.navn} gradering={deltaker.gradering} />
                  <BodyShort>{deltaker.norskIdent}</BodyShort>
                </VStack>
              </Table.HeaderCell>
              <Table.DataCell>{deltaker.oppfolgingEnhet?.navn ?? "-"}</Table.DataCell>
              <Table.DataCell>
                <Tooltip content={deltaker.innholdAnnet ?? ""}>
                  <BodyShort>{truncate(deltaker.innholdAnnet ?? "", 30)}</BodyShort>
                </Tooltip>
              </Table.DataCell>
              <Table.DataCell>{formaterDato(deltaker.startDato)}</Table.DataCell>
              <Table.DataCell>{formaterDato(deltaker.sluttDato)}</Table.DataCell>
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

function truncate(text: string, maxLength: number): string {
  return text.length > maxLength ? `${text.substring(0, maxLength - 3)}...` : text;
}
