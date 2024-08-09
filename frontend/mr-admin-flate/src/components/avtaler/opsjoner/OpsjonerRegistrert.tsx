import { Alert, BodyShort, Button, Heading, HStack, Table } from "@navikt/ds-react";
import { Avtale, OpsjonLoggRegistrert, OpsjonStatus } from "mulighetsrommet-api-client";
import { useSlettOpsjon } from "../../../api/avtaler/useSlettOpsjon";
import { formaterDato } from "../../../utils/Utils";
import styles from "./OpsjonerRegistrert.module.scss";

interface Props {
  avtale: Avtale;
  readOnly: boolean;
}

export function OpsjonerRegistrert({ avtale, readOnly }: Props) {
  const logg = avtale.opsjonerRegistrert;
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
        },
      },
    );
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
                <Table.DataCell>{formaterStatus(log)}</Table.DataCell>
                <Table.DataCell>
                  {kanSletteOpsjon(log) && !readOnly ? (
                    <>
                      <Button
                        onClick={() => fjernOpsjon(log.id)}
                        size="small"
                        variant="primary"
                        type="button"
                        className={styles.button_as_link}
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
