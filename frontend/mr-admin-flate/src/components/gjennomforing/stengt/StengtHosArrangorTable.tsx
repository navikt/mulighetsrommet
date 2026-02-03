import { useDeleteStengtHosArrangor } from "@/api/gjennomforing/useDeleteStengtHosArrangor";
import { QueryKeys } from "@/api/QueryKeys";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { TrashIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, Table } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { isGruppetiltak } from "@/api/gjennomforing/utils";

interface StengtHosArrangorTableProps {
  gjennomforingId: string;
  readOnly?: boolean;
}

export function StengtHosArrangorTable({ gjennomforingId, readOnly }: StengtHosArrangorTableProps) {
  const { gjennomforing } = useGjennomforing(gjennomforingId);
  const deleteStengtHosArrangor = useDeleteStengtHosArrangor(gjennomforingId);
  const queryClient = useQueryClient();

  if (!isGruppetiltak(gjennomforing) || gjennomforing.stengt.length === 0) {
    return null;
  }

  function deleteStengtPeriode(periodeId: number) {
    deleteStengtHosArrangor.mutate(periodeId, {
      onSuccess: async () => {
        await queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforing(gjennomforingId),
          refetchType: "all",
        });
      },
    });
  }

  return (
    <section className="bg-surface-subtle p-4 rounded-lg">
      <HStack justify={"space-between"} align={"center"}>
        <Heading level="4" size="xsmall">
          Perioder hvor tiltakstilbudet er stengt hos arrangør
        </Heading>
      </HStack>
      <hr className="h-[0.2rem] bg-border-strong border-none" />
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Periode</Table.HeaderCell>
            <Table.HeaderCell>Beskrivelse</Table.HeaderCell>
            {!readOnly && <Table.HeaderCell></Table.HeaderCell>}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {gjennomforing.stengt.map((periode) => {
            return (
              <Table.Row key={periode.id}>
                <Table.DataCell>{`${formaterDato(periode.start)} - ${formaterDato(periode.slutt)}`}</Table.DataCell>
                <Table.DataCell>{periode.beskrivelse}</Table.DataCell>
                {!readOnly && (
                  <Table.DataCell>
                    <Button
                      type="button"
                      size="small"
                      variant="secondary-neutral"
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
