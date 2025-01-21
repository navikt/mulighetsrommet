import { Alert, BodyShort, Button, Heading, HStack, Table, VStack } from "@navikt/ds-react";
import { AvtaleDto, OpsjonLoggRegistrert, OpsjonStatus } from "@mr/api-client";
import { useSlettOpsjon } from "@/api/avtaler/useSlettOpsjon";
import { formaterDato } from "@/utils/Utils";
import { useRevalidator } from "react-router";
import { TrashIcon } from "@navikt/aksel-icons";

interface Props {
  avtale: AvtaleDto;
  readOnly: boolean;
}

export function OpsjonerRegistrert({ avtale, readOnly }: Props) {
  const logg = avtale.opsjonerRegistrert;
  const revalidator = useRevalidator();
  const mutation = useSlettOpsjon();

  function kanSletteOpsjon(opsjon: OpsjonLoggRegistrert): boolean {
    const sisteUtlosteOpsjon = logg.at(-1);

    return opsjon.id === sisteUtlosteOpsjon?.id;
  }

  function fjernOpsjon(id: string) {
    mutation.mutate(
      { id, avtaleId: avtale.id },
      {
        onSuccess: () => {
          mutation.reset();
          revalidator.revalidate();
        },
      },
    );
  }

  const opprinneligSluttDato = avtale.opsjonerRegistrert
    .filter((o) => o.status === OpsjonStatus.OPSJON_UTLØST && !!o.forrigeSluttdato)
    .sort(
      (a, b) => new Date(a.forrigeSluttdato!).getTime() - new Date(b.forrigeSluttdato!).getTime(),
    )
    .at(0)?.forrigeSluttdato;

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
                <Table.DataCell>{formaterDato(log.aktivertDato)}</Table.DataCell>
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
                      {mutation.error && (
                        <VStack>
                          <Alert inline variant="error">
                            Klarte ikke fjerne opsjonen
                          </Alert>
                          <Button size="small" type="button" onClick={() => fjernOpsjon(log.id)}>
                            Prøv igjen
                          </Button>
                        </VStack>
                      )}
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

function formaterStatus(log: OpsjonLoggRegistrert): string {
  if (log.sluttDato && log.status === OpsjonStatus.OPSJON_UTLØST) {
    return formaterDato(log.sluttDato);
  } else if (log.status === OpsjonStatus.SKAL_IKKE_UTLØSE_OPSJON) {
    return "Avklart at opsjon ikke skal utløses";
  } else if (log.status === OpsjonStatus.PÅGÅENDE_OPSJONSPROSESS) {
    return "Opsjonsprosessen er pågående";
  }

  return "";
}
