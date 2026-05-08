import { NavnOgGradering } from "@/components/personalia/NavnOgGradering";
import { DataElementStatusTag } from "@mr/frontend-common";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { CheckmarkCircleIcon } from "@navikt/aksel-icons";
import { BodyShort, Box, Table, VStack } from "@navikt/ds-react";
import { TilsagnDeltakerDto } from "@tiltaksadministrasjon/api-client";
import { useState } from "react";

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
                <InnholdAnnetCell text={deltaker.innholdAnnet ?? ""} />
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

const MAX_LENGTH = 30;

function InnholdAnnetCell({ text }: { text: string }) {
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);
  const truncated = text.length > MAX_LENGTH ? `${text.substring(0, MAX_LENGTH - 3)}...` : text;

  return (
    <div
      style={{ display: "inline-block" }}
      onMouseEnter={(e) => {
        const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
        setPos({ top: rect.top, left: rect.left });
      }}
      onMouseLeave={() => setPos(null)}
    >
      <BodyShort>{truncated}</BodyShort>
      {pos && text.length > MAX_LENGTH && (
        <Box
          background="default"
          borderColor="neutral"
          borderRadius="8"
          borderWidth="1"
          shadow="dialog"
          padding="space-4"
          style={{
            position: "fixed",
            top: pos.top,
            left: pos.left,
            zIndex: 1000,
            whiteSpace: "pre-wrap",
            width: "320px",
          }}
        >
          <BodyShort>{text}</BodyShort>
        </Box>
      )}
    </div>
  );
}
