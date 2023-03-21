import { Search, Select } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Avtalestatus, SorteringAvtaler } from "mulighetsrommet-api-client";
import { ChangeEvent, useEffect, useRef } from "react";
import { avtaleFilter, avtalePaginationAtom } from "../../api/atoms";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { useEnheter } from "../../api/enhet/useEnheter";
import styles from "./Filter.module.scss";
import { resetPaginering } from "../../utils/Utils";

export function Avtalefilter() {
  const [filter, setFilter] = useAtom(avtaleFilter);
  const { data: enheter } = useEnheter();
  const { data } = useAvtaler();
  const [, setPage] = useAtom(avtalePaginationAtom);
  const searchRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    // Hold fokus på søkefelt dersom bruker skriver i søkefelt
    if (filter.sok !== "") {
      searchRef?.current?.focus();
    }
  }, [data]);

  return (
    <>
      <div className={styles.filter_container}>
        <div className={styles.filter_left}>
          <Search
            ref={searchRef}
            label="Søk etter avtale"
            hideLabel
            variant="simple"
            onChange={(sok: string) => {
              setFilter({ ...filter, sok });
            }}
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
              resetPaginering(setPage);
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
            <option value="">Alle statuser</option>
          </Select>
          <Select
            label="Filtrer på enhet"
            hideLabel
            size="small"
            value={filter.enhet}
            data-testid="filter_avtale_enhet"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              resetPaginering(setPage);
              setFilter({ ...filter, enhet: e.currentTarget.value });
            }}
          >
            <option value="">Alle enheter</option>
            {enheter?.map((enhet) => (
              <option key={enhet.enhetId} value={enhet.enhetNr}>
                {enhet.navn} - {enhet.enhetNr}
              </option>
            ))}
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
              setFilter({
                ...filter,
                sortering: e.currentTarget.value as SorteringAvtaler,
              });
            }}
          >
            <option value="navn-ascending">Navn A-Å</option>
            <option value="navn-descending">Navn Å-A</option>
            <option value="status-ascending">Status A-Å</option>
            <option value="status-descending">Status Å-A</option>
          </Select>
        </div>
      </div>
    </>
  );
}
