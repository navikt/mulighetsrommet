import { Alert, BodyShort, Button, Heading, HStack, Table } from "@navikt/ds-react";
import { Avtale, OpsjonLoggRegistrert, OpsjonStatus } from "mulighetsrommet-api-client";
import { formaterDato } from "../../../utils/Utils";
import styles from "./OpsjonerRegistrert.module.scss";
import { useSlettOpsjon } from "../../../api/avtaler/useSlettOpsjon";
import { useEffect } from "react";

interface Props {
  avtale: Avtale;
  readOnly: boolean;
}

export function OpsjonerRegistrert({ avtale, readOnly }: Props) {
  const logg = avtale.opsjonerRegistrert;
  const mutation = useSlettOpsjon();

  useEffect(() => {
    if (mutation.isSuccess) {
      mutation.reset();
    }
  }, [mutation]);

  function kanSletteOpsjon(opsjon: OpsjonLoggRegistrert): boolean {
    const sisteUtlosteOpsjon = logg
      .filter((log) => log.status === OpsjonStatus.OPSJON_UTLØST)
      .at(-1);

    return opsjon.id === sisteUtlosteOpsjon?.id;
  }

  function fjernSisteOpsjon(id: string) {
    mutation.mutate({ id, avtaleId: avtale.id });
  }

  return (
    <section className={styles.container}>
      <HStack justify={"space-between"} align={"center"}>
        <Heading level="4" size="xsmall">
          Opsjoner
        </Heading>
        {avtale.opprinneligSluttDato && (
          <BodyShort>
            * Opprinnelig sluttdato: {formaterDato(avtale.opprinneligSluttDato)}
          </BodyShort>
        )}
      </HStack>
      <hr className={styles.separator} />
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
                <Table.DataCell>{formaterDato(log.sluttDato)}</Table.DataCell>
                <Table.DataCell>
                  {kanSletteOpsjon(log) && !readOnly ? (
                    <>
                      <Button
                        onClick={() => fjernSisteOpsjon(log.id)}
                        size="small"
                        variant="primary"
                      >
                        Fjern
                      </Button>
                      {mutation.error && (
                        <Alert inline variant="error">
                          Klarte ikke fjerne opsjonen
                        </Alert>
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
