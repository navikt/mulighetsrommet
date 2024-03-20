import { Alert, Table } from "@navikt/ds-react";
import classNames from "classnames";
import { useAtom } from "jotai";
import { SorteringTiltakstyper } from "mulighetsrommet-api-client";
import { Lenke } from "mulighetsrommet-frontend-common/components/lenke/Lenke";
import { tiltakstypeFilterAtom } from "../../api/atoms";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useSort } from "../../hooks/useSort";
import { formaterDato } from "../../utils/Utils";
import { Laster } from "../laster/Laster";
import { TiltakstypestatusTag } from "../statuselementer/TiltakstypestatusTag";
import styles from "./Tabell.module.scss";

export const TiltakstypeTabell = () => {
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
      sortering: `${sortKey}-${direction}` as SorteringTiltakstyper,
    });
  };
  return (
    <div className={classNames(styles.tabell_wrapper, styles.tiltakstypetabell)}>
      <Table
        sort={sort!}
        onSortChange={(sortKey) => handleSort(sortKey!)}
        className={styles.tabell}
      >
        <Table.Header>
          <Table.Row className={styles.tiltakstype_tabellrad}>
            <Table.ColumnHeader sortKey="navn" sortable>
              Navn
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="startdato" sortable>
              Startdato
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="sluttdato" sortable>
              Sluttdato
            </Table.ColumnHeader>
            <Table.ColumnHeader>Status</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        {tiltakstyper.length > 0 ? (
          <Table.Body>
            {tiltakstyper.map((tiltakstype, index) => {
              return (
                <Table.Row key={index} className={styles.tiltakstype_tabellrad}>
                  <Table.DataCell
                    aria-label={`Navn på tiltakstype: ${tiltakstype.navn}`}
                    className={styles.title}
                  >
                    <Lenke to={`/tiltakstyper/${tiltakstype.id}`}>{tiltakstype.navn}</Lenke>
                  </Table.DataCell>
                  <Table.DataCell aria-label={`Startdato: ${formaterDato(tiltakstype.fraDato)}`}>
                    {formaterDato(tiltakstype.fraDato)}
                  </Table.DataCell>
                  <Table.DataCell aria-label={`Sluttdato: ${formaterDato(tiltakstype.tilDato)}`}>
                    {formaterDato(tiltakstype.tilDato)}
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
    </div>
  );
};
