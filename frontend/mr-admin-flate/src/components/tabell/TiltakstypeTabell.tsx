import { Alert, Table } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { SorteringTiltakstyper } from "mulighetsrommet-api-client";
import { Lenke } from "mulighetsrommet-frontend-common/components/lenke/Lenke";
import { tiltakstypeFilterAtom } from "@/api/atoms";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useSort } from "@/hooks/useSort";
import { formaterDato } from "@/utils/Utils";
import { Laster } from "../laster/Laster";
import { TiltakstypestatusTag } from "../statuselementer/TiltakstypestatusTag";
import styles from "./Tabell.module.scss";
import { TabellWrapper } from "@/components/tabell/TabellWrapper";

export function TiltakstypeTabell() {
  const [filter, setFilter] = useAtom(tiltakstypeFilterAtom);
  const { data, isLoading } = useTiltakstyper(filter);
  const [sort, setSort] = useSort("navn");
  const tiltakstyper = data?.data ?? [];

  if ((!tiltakstyper || tiltakstyper.length === 0) && isLoading) {
    return <Laster size="xlarge" tekst="Laster tiltakstyper..." />;
  }

  if (tiltakstyper.length === 0) {
    return <Alert variant="info">Fant ingen tiltakstyper</Alert>;
  }

  const handleSort = (sortKey: string) => {
    const direction = sort.direction === "ascending" ? "descending" : "ascending";

    setSort({
      orderBy: sortKey,
      direction,
    });

    setFilter({
      ...filter,
      sort: `${sortKey}-${direction}` as SorteringTiltakstyper,
    });
  };
  return (
    <TabellWrapper className={styles.tiltakstypetabell}>
      <Table
        sort={sort!}
        onSortChange={(sortKey) => handleSort(sortKey!)}
        className={styles.tabell}
      >
        <Table.Header>
          <Table.Row>
            {headers.map((header) => (
              <Table.ColumnHeader
                key={header.sortKey}
                sortKey={header.sortKey}
                sortable={header.sortable}
                style={{
                  width: header.width,
                }}
              >
                {header.tittel}
              </Table.ColumnHeader>
            ))}
          </Table.Row>
        </Table.Header>
        {tiltakstyper.length > 0 ? (
          <Table.Body>
            {tiltakstyper.map((tiltakstype, index) => {
              const startDato = formaterDato(tiltakstype.startDato);
              const sluttDato = tiltakstype.sluttDato ? formaterDato(tiltakstype.sluttDato) : "-";
              return (
                <Table.Row key={index} className={styles.tiltakstype_tabellrad}>
                  <Table.DataCell
                    aria-label={`Navn på tiltakstype: ${tiltakstype.navn}`}
                    className={styles.title}
                  >
                    <Lenke to={`/tiltakstyper/${tiltakstype.id}`}>{tiltakstype.navn}</Lenke>
                  </Table.DataCell>
                  <Table.DataCell aria-label={`Startdato: ${startDato}`}>
                    {startDato}
                  </Table.DataCell>
                  <Table.DataCell aria-label={`Sluttdato: ${sluttDato}`}>
                    {sluttDato}
                  </Table.DataCell>
                  <Table.DataCell>
                    <TiltakstypestatusTag tiltakstype={tiltakstype} />
                  </Table.DataCell>
                </Table.Row>
              );
            })}
          </Table.Body>
        ) : (
          <></>
        )}
      </Table>
    </TabellWrapper>
  );
}

interface ColumnHeader {
  sortKey: Kolonne;
  tittel: string;
  sortable: boolean;
  width: string;
}

const headers: ColumnHeader[] = [
  {
    sortKey: "navn",
    tittel: "Navn",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "startdato",
    tittel: "Startdato",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "sluttdato",
    tittel: "Sluttdato",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "status",
    tittel: "Status",
    sortable: false,
    width: "1fr",
  },
];

type Kolonne =
  | "navn"
  | "avtalenummer"
  | "arrangor"
  | "region"
  | "startdato"
  | "sluttdato"
  | "status";
