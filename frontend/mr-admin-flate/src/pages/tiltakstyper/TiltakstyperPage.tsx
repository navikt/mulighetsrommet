import { Heading, Search, Select } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { ChangeEvent } from "react";
import { tiltakstypefilter, paginationAtom } from "../../api/atoms";
import { TiltakstyperOversikt } from "../../components/tiltakstyper/TiltakstyperOversikt";
import { MainContent } from "../../components/visuals/MainContent";
import { NavigeringHeader } from "../forside/NavigeringHeader";
import styles from "../Page.module.scss";

export function TiltakstyperPage() {
  const [sokefilter, setSokefilter] = useAtom(tiltakstypefilter);
  const [, setPage] = useAtom(paginationAtom);

  const resetPagination = () => {
    setPage(1);
  };

  return (
    <>
      <NavigeringHeader />
      <MainContent>
        <div className={styles.header_wrapper}>
          <Heading level="2" size="large">
            Oversikt over tiltakstyper
          </Heading>
        </div>
        <div className={styles.filterseksjon}>
          <Search
            label="Søk etter tiltakstype"
            hideLabel={false}
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
            value={sokefilter.status}
            data-testid="filter_status"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              resetPagination();
              setSokefilter({
                ...sokefilter,
                status: e.currentTarget.value as any,
              });
            }}
          >
            <option value="AKTIV">Aktiv</option>
            <option value="PLANLAGT">Planlagt</option>
            <option value="AVSLUTTET">Avsluttet</option>
            <option value="ALLE">Alle</option>
          </Select>
          <Select
            label="Gruppetiltak eller individuelle tiltak"
            size="small"
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
        <TiltakstyperOversikt />
      </MainContent>
    </>
  );
}
