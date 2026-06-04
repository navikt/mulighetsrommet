import { useDeleteStengtHosArrangor } from "@/api/gjennomforing/useDeleteStengtHosArrangor";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { TrashIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, Table } from "@navikt/ds-react";
import { GjennomforingDtoStengtPeriode } from "@tiltaksadministrasjon/api-client";

interface StengtHosArrangorTableProps {
  gjennomforingId: string;
  stengt: GjennomforingDtoStengtPeriode[];
  readOnly?: boolean;
}

export function StengtHosArrangorTable({
  gjennomforingId,
  stengt,
  readOnly,
}: StengtHosArrangorTableProps) {
  const deleteStengtHosArrangor = useDeleteStengtHosArrangor(gjennomforingId);

  if (stengt.length === 0) {
    return null;
  }

  function deleteStengtPeriode(periodeId: number) {
    deleteStengtHosArrangor.mutate(periodeId);
  }

  return (
    <section className="bg-ax-bg-neutral-soft p-4 rounded-lg">
      <HStack justify={"space-between"} align={"center"}>
        <Heading level="4" size="xsmall">
          Perioder hvor tiltakstilbudet er stengt hos arrangør
        </Heading>
      </HStack>
      <hr className="h-[0.2rem] bg-ax-border-neutral-strong border-none" />
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Periode</Table.HeaderCell>
            <Table.HeaderCell>Beskrivelse</Table.HeaderCell>
            {!readOnly && <Table.HeaderCell></Table.HeaderCell>}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {stengt.map((periode) => {
            return (
              <Table.Row key={periode.id}>
                <Table.DataCell>{`${formaterDato(periode.start)} - ${formaterDato(periode.slutt)}`}</Table.DataCell>
                <Table.DataCell>{periode.beskrivelse}</Table.DataCell>
                {!readOnly && (
                  <Table.DataCell>
                    <Button
                      data-color="neutral"
                      type="button"
                      size="small"
                      variant="secondary"
                      icon={<TrashIcon aria-hidden />}
                      onClick={() => deleteStengtPeriode(periode.id)}
                    >
                      Slett
                    </Button>
                  </Table.DataCell>
                )}
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </section>
  );
}
