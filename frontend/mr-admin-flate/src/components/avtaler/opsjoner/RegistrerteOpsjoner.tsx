import { useAvtale } from "@/api/avtaler/useAvtale";
import { useSlettOpsjon } from "@/api/avtaler/useSlettOpsjon";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { compare, formaterDato } from "@mr/frontend-common/utils/date";
import { TrashIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading, HStack, Table } from "@navikt/ds-react";
import { AvtaleOpsjonLoggDto, OpsjonLoggStatus } from "@tiltaksadministrasjon/api-client";

interface Props {
  readOnly: boolean;
}

export function RegistrerteOpsjoner({ readOnly }: Props) {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);
  const logg = avtale.opsjonerRegistrert;
  const mutation = useSlettOpsjon(avtale.id);

  function kanSletteOpsjon(opsjon: AvtaleOpsjonLoggDto): boolean {
    const sisteUtlosteOpsjon = logg.at(-1);

    return opsjon.id === sisteUtlosteOpsjon?.id;
  }

  function fjernOpsjon(id: string) {
    mutation.mutate(id, {
      onSuccess: () => {
        mutation.reset();
      },
    });
  }

  const opprinneligSluttDato = avtale.opsjonerRegistrert
    .filter((o) => o.status === OpsjonLoggStatus.OPSJON_UTLOST && !!o.forrigeSluttDato)
    .sort((a, b) => compare(a.forrigeSluttDato, b.forrigeSluttDato))
    .at(0)?.forrigeSluttDato;

  return (
    <section className="bg-surface-subtle p-4 rounded-lg">
      <HStack justify={"space-between"} align={"center"}>
        <Heading level="4" size="xsmall">
          Opsjoner
        </Heading>
        {opprinneligSluttDato && (
          <BodyShort>* Opprinnelig sluttdato: {formaterDato(opprinneligSluttDato)}</BodyShort>
        )}
      </HStack>
      <hr className="h-[0.2rem] bg-border-strong border-none" />
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Aktivert</Table.HeaderCell>
            <Table.HeaderCell>Opsjon utløst til</Table.HeaderCell>
            <Table.HeaderCell></Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {logg.map((log) => {
            return (
              <Table.Row key={log.id}>
                <Table.DataCell>{formaterDato(log.createdAt)}</Table.DataCell>
                <Table.DataCell>{formaterStatus(log)}</Table.DataCell>
                <Table.DataCell>
                  {kanSletteOpsjon(log) && !readOnly ? (
                    <>
                      <Button
                        type="button"
                        size="small"
                        variant="secondary-neutral"
                        icon={<TrashIcon aria-hidden />}
                        onClick={() => fjernOpsjon(log.id)}
                      >
                        Fjern
                      </Button>
                    </>
                  ) : null}
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </section>
  );
}

function formaterStatus(log: AvtaleOpsjonLoggDto): string {
  switch (log.status) {
    case OpsjonLoggStatus.OPSJON_UTLOST:
      return formaterDato(log.sluttDato) ?? "-";
    case OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON:
      return "Avklart at opsjon ikke skal utløses";
  }
}
