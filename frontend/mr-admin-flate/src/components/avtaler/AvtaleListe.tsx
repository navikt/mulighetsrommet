import { Alert, Table } from "@navikt/ds-react";
import { Laster } from "../laster/Laster";
import { AvtaleFilter } from "@/api/atoms";
import { AvtaleDto } from "@mr/api-client";
import { ReactNode } from "react";
import { useAvtaler } from "@/api/avtaler/useAvtaler";
import { AvtalestatusTag } from "../statuselementer/AvtalestatusTag";

interface Props {
  filter: Partial<AvtaleFilter>;
  action: (avtale: AvtaleDto) => ReactNode;
}

export function AvtaleListe(props: Props) {
  const { data, isError, isPending } = useAvtaler(props.filter);

  if (isError) {
    return <Alert variant="error">Vi hadde problemer med å hente avtaler</Alert>;
  }

  if (isPending) {
    return <Laster size="xlarge" tekst="Laster avtaler..." />;
  }

  const avtaler = data.data;

  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Tittel</Table.HeaderCell>
          <Table.HeaderCell scope="col">Avtalenummer</Table.HeaderCell>
          <Table.HeaderCell scope="col">Status</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {avtaler.map((avtale) => (
          <Table.Row key={avtale.id}>
            <Table.DataCell>{avtale.navn}</Table.DataCell>
            <Table.DataCell>{avtale.avtalenummer}</Table.DataCell>
            <Table.DataCell>
              {" "}
              <AvtalestatusTag avtale={avtale} />
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
