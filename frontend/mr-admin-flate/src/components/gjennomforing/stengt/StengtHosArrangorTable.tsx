import { GjennomforingDto } from "@mr/api-client-v2";
import { Alert, Button, Heading, HStack, Table, VStack } from "@navikt/ds-react";
import { formaterDato } from "@/utils/Utils";
import { useRevalidator } from "react-router";
import { useDeleteStengtHosArrangor } from "@/api/gjennomforing/useDeleteStengtHosArrangor";
import { TrashIcon } from "@navikt/aksel-icons";

interface StengtHosArrangorTableProps {
  gjennomforing: GjennomforingDto;
  readOnly?: boolean;
}

export function StengtHosArrangorTable({ gjennomforing, readOnly }: StengtHosArrangorTableProps) {
  const deleteStengtHosArrangor = useDeleteStengtHosArrangor(gjennomforing.id);
  const revalidator = useRevalidator();

  return (
    <section className="bg-surface-subtle p-4 rounded-lg">
      <HStack justify={"space-between"} align={"center"}>
        <Heading level="4" size="xsmall">
          Perioder med stengt hos arrangør
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
                      onClick={() => {
                        deleteStengtHosArrangor.mutate(periode.id, {
                          onSuccess: async () => {
                            await revalidator.revalidate();
                          },
                        });
                      }}
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

      {deleteStengtHosArrangor.error && (
        <VStack>
          <Alert inline variant="error">
            Klarte ikke slette periode
          </Alert>
        </VStack>
      )}
    </section>
  );
}
