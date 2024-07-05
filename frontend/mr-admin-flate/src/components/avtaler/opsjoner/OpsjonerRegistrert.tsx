import { BodyShort, Heading, HStack, Table } from "@navikt/ds-react";
import { Avtale } from "mulighetsrommet-api-client";
import { formaterDato } from "../../../utils/Utils";
import styles from "./OpsjonerRegistrert.module.scss";

interface Props {
  avtale: Avtale;
}

export function OpsjonerRegistrert({ avtale }: Props) {
  const logg = avtale.opsjonerRegistrert;

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
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {logg.map((log) => {
            return (
              <Table.Row key={log.id}>
                <Table.DataCell>{formaterDato(log.aktivertDato)}</Table.DataCell>
                <Table.DataCell>{formaterDato(log.sluttDato)}</Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </section>
  );
}
