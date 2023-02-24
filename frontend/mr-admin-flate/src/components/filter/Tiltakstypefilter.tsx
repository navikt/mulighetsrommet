import { Search, Select } from "@navikt/ds-react";
import classNames from "classnames";
import { useAtom } from "jotai";
import { SorteringTiltakstyper } from "mulighetsrommet-api-client";
import { ChangeEvent } from "react";
import { tiltakstypefilter, paginationAtom } from "../../api/atoms";
import styles from "./Filter.module.scss";

export function Tiltakstypefilter() {
  const [sokefilter, setSokefilter] = useAtom(tiltakstypefilter);
  const [, setPage] = useAtom(paginationAtom);

  const resetPagination = () => {
    setPage(1);
  };

  return (
    <div className={classNames(styles.filter_container)}>
      <div className={styles.filter_left}>
        <Search
          label="Søk etter tiltakstype"
          hideLabel
          variant="simple"
          onChange={(sok: string) => setSokefilter({ ...sokefilter, sok })}
          value={sokefilter.sok}
          aria-label="Søk etter tiltakstype"
          data-testid="filter_sokefelt"
          size="small"
        />
        <Select
          label="Filtrer på statuser"
          size="small"
          hideLabel
          value={sokefilter.status}
          data-testid="filter_status"
          onChange={(e: ChangeEvent<HTMLSelectElement>) => {
            resetPagination();
            const status = e.currentTarget.value as any;
            setSokefilter({
              ...sokefilter,
              status: status === "Alle" ? undefined : status,
            });
          }}
        >
          <option value="Aktiv">Aktiv</option>
          <option value="Planlagt">Planlagt</option>
          <option value="Avsluttet">Avsluttet</option>
          <option value="Alle">Alle</option>
        </Select>
        <Select
          label="Gruppetiltak eller individuelle tiltak"
          size="small"
          hideLabel
          value={sokefilter.kategori}
          data-testid="filter_kategori"
          onChange={(e: ChangeEvent<HTMLSelectElement>) => {
            resetPagination();
            const kategori = e.currentTarget.value as any;
            setSokefilter({
              ...sokefilter,
              kategori: kategori === "ALLE" ? undefined : kategori,
            });
          }}
        >
          <option value="GRUPPE">Gruppetiltak</option>
          <option value="INDIVIDUELL">Individuelle tiltak</option>
          <option value="ALLE">Alle</option>
        </Select>
      </div>
      <div>
        <Select
          label="Sorter"
          hideLabel
          size="small"
          value={sokefilter.sortering}
          data-testid="filter_avtale_enhet"
          onChange={(e: ChangeEvent<HTMLSelectElement>) => {
            setSokefilter({
              ...sokefilter,
              sortering: e.currentTarget.value as SorteringTiltakstyper,
            });
          }}
        >
          <option value="navn-ascending">Navn A-Å</option>
          <option value="navn-descending">Navn Å-A</option>
        </Select>
      </div>
    </div>
  );
}
