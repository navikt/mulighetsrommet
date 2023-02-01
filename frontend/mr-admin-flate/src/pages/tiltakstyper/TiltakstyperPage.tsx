import { Heading, Search, Select } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { ChangeEvent } from "react";
import { tiltakstypefilter } from "../../api/atoms";
import { TiltakstyperOversikt } from "../../components/tiltakstyper/TiltakstyperOversikt";
import styles from "../Page.module.scss";

export function TiltakstyperPage() {
  const [sokefilter, setSokefilter] = useAtom(tiltakstypefilter);

  return (
    <>
      <div className={styles.header_wrapper}>
        <Heading size="large">Oversikt over tiltakstyper</Heading>
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
          onChange={(e: ChangeEvent<HTMLSelectElement>) =>
            setSokefilter({
              ...sokefilter,
              status: e.currentTarget.value as any,
            })
          }
        >
          <option value="AKTIV">Aktive</option>
          <option value="PLANLAGT">Planlagte</option>
          <option value="AVSLUTTET">Avsluttede</option>
        </Select>
      </div>
      <TiltakstyperOversikt />
    </>
  );
}
