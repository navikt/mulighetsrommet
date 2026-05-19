import { NavnOgGradering } from "@/components/personalia/NavnOgGradering";
import { DataElementStatusTag } from "@mr/frontend-common";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { BodyShort, Checkbox, Table } from "@navikt/ds-react";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { TilsagnDeltakerDto, TilsagnDeltakerRequest } from "@tiltaksadministrasjon/api-client";
import { useState } from "react";
import { ChevronDownIcon, ChevronUpIcon } from "@navikt/aksel-icons";

interface Props {
  deltakere: TilsagnDeltakerDto[];
  selected: TilsagnDeltakerRequest[];
  setSelected?: (a: TilsagnDeltakerRequest[]) => void;
  editable?: boolean;
}

export function TilsagnDeltakereTable({ deltakere, selected, setSelected }: Props) {
  const allSelected = selected.length === deltakere.length;
  const indeterminate = selected.length > 0 && selected.length !== deltakere.length;

  return (
    <Table>
      <Table.Header>
        <Table.Row>
          {setSelected && (
            <Table.DataCell>
              <Checkbox
                checked={allSelected}
                indeterminate={indeterminate}
                onChange={() =>
                  allSelected
                    ? setSelected([])
                    : setSelected(
                        deltakere.map((d) => ({
                          deltakerId: d.deltakerId,
                          innholdAnnet: d.innholdAnnet,
                        })),
                      )
                }
                hideLabel
              >
                Velg alle rader
              </Checkbox>
            </Table.DataCell>
          )}
          <Table.HeaderCell scope="col">Deltaker</Table.HeaderCell>
          <Table.HeaderCell scope="col">Oppfølgingsenhet</Table.HeaderCell>
          <Table.HeaderCell scope="col">Innhold</Table.HeaderCell>
          <Table.HeaderCell scope="col">Start</Table.HeaderCell>
          <Table.HeaderCell scope="col">Slutt</Table.HeaderCell>
          <Table.HeaderCell scope="col">Status</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {deltakere.map((deltaker) => {
          return (
            <Table.Row key={deltaker.deltakerId}>
              {setSelected && (
                <Table.DataCell>
                  <Checkbox
                    checked={selected.some((s) => s.deltakerId === deltaker.deltakerId)}
                    onChange={() =>
                      setSelected(
                        addOrRemove(selected, {
                          deltakerId: deltaker.deltakerId,
                          innholdAnnet: deltaker.innholdAnnet,
                        }),
                      )
                    }
                    hideLabel
                  >
                    {`Velg deltaker ${deltaker.navn}`}
                  </Checkbox>
                </Table.DataCell>
              )}
              <Table.HeaderCell scope="row">
                <NavnOgGradering
                  navn={deltaker.navn}
                  gradering={deltaker.gradering}
                  norskIdent={deltaker.norskIdent}
                />
              </Table.HeaderCell>
              <Table.DataCell>{deltaker.oppfolgingEnhet?.navn ?? "-"}</Table.DataCell>
              <Table.DataCell>
                {deltaker.innholdAnnet && <InnholdCell innhold={deltaker.innholdAnnet} />}
              </Table.DataCell>
              <Table.DataCell>{formaterDato(deltaker.startDato)}</Table.DataCell>
              <Table.DataCell>{formaterDato(deltaker.sluttDato)}</Table.DataCell>
              <Table.DataCell>
                <DataElementStatusTag {...deltaker.status} />
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}

const TRUNCATE_INNHOLD_AT = 60;

function InnholdCell({ innhold }: { innhold: string }) {
  const [expanded, setExpanded] = useState(false);
  const needsTruncation = innhold.length > TRUNCATE_INNHOLD_AT;

  if (!needsTruncation) {
    return <BodyShort>{innhold}</BodyShort>;
  }

  return (
    <div className="flex flex-col items-center">
      <BodyShort className="self-stretch whitespace-pre-wrap">
        {expanded ? innhold : `${innhold.slice(0, TRUNCATE_INNHOLD_AT).trimEnd()}...`}
      </BodyShort>
      <button
        type="button"
        onClick={() => setExpanded(!expanded)}
        className="mt-1 cursor-pointer text-blue-600 hover:text-blue-800"
        aria-expanded={expanded}
      >
        {expanded ? (
          <ChevronUpIcon
            style={{ color: "var(--ax-text-info-decoration)" }}
            title="Vis mindre"
            fontSize="1.5rem"
          />
        ) : (
          <ChevronDownIcon
            style={{ color: "var(--ax-text-info-decoration)" }}
            title="Vis mer"
            fontSize="1.5rem"
          />
        )}
      </button>
    </div>
  );
}
