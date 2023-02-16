import { Search, Select } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Avtalestatus } from "mulighetsrommet-api-client";
import { ChangeEvent } from "react";
import { avtaleFilter } from "../../api/atoms";
import styles from "./Avtalefilter.module.scss";

export function Avtalefilter() {
  const [filter, setFilter] = useAtom(avtaleFilter);
  return (
    <>
      <div className={styles.filter_container}>
        <div className={styles.filter_left}>
          <Search
            label="Søk etter avtale"
            hideLabel
            variant="simple"
            onChange={(sok: string) => setFilter({ ...filter, sok })}
            value={filter.sok}
            aria-label="Søk etter avtale"
            data-testid="filter_avtale_sokefelt"
            size="small"
          />
          <Select
            label="Filtrer på statuser"
            hideLabel
            size="small"
            value={filter.status}
            data-testid="filter_avtale_status"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              setFilter({
                ...filter,
                status: e.currentTarget.value as Avtalestatus,
              });
            }}
          >
            <option value="Aktiv">Aktiv</option>
            <option value="Planlagt">Planlagt</option>
            <option value="Avsluttet">Avsluttet</option>
            <option value="Avbrutt">Avbrutt</option>
          </Select>
          <Select
            label="Enhet"
            hideLabel
            size="small"
            value={filter.enhet}
            data-testid="filter_avtale_enhet"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              setFilter({ ...filter, enhet: e.currentTarget.value });
            }}
          >
            <option value="ALLE">Alle</option>
          </Select>
        </div>
        <div>
          <Select
            label="Sorter"
            hideLabel
            size="small"
            value={filter.sortering}
            data-testid="filter_avtale_enhet"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              setFilter({ ...filter, sortering: e.currentTarget.value });
            }}
          >
            <option value="">Sorter</option>
          </Select>
        </div>
      </div>
    </>
  );
}
