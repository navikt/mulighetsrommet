import { Search, Select } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  SorteringTiltaksgjennomforinger,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { ChangeEvent } from "react";
import { paginationAtom, tiltaksgjennomforingfilter } from "../../api/atoms";
import { useEnheter } from "../../api/enhet/useEnheter";
import { useAlleTiltakstyper } from "../../api/tiltakstyper/useAlleTiltakstyper";
import { resetPaginering } from "../../utils/Utils";
import styles from "./Filter.module.scss";

export function Tiltaksgjennomforingfilter() {
  const [sokefilter, setSokefilter] = useAtom(tiltaksgjennomforingfilter);
  const [, setPage] = useAtom(paginationAtom);
  const { data: enheter } = useEnheter();
  const { data: tiltakstyper } = useAlleTiltakstyper({
    tiltakstypestatus: Tiltakstypestatus.AKTIV,
  });

  return (
    <form className={styles.filter_container}>
      <div className={styles.filter_left}>
        <Search
          label="Søk etter tiltaksgjennomføring"
          hideLabel
          variant="simple"
          onChange={(search: string) =>
            setSokefilter({ ...sokefilter, search })
          }
          value={sokefilter.search}
          aria-label="Søk etter tiltaksgjennomføring"
          data-testid="filter_sokefelt"
          size="small"
        />
        <Select
          label="Filtrer på enhet"
          hideLabel
          size="small"
          value={sokefilter.enhet}
          data-testid="filter_tiltaksgjennomforing_enhet"
          onChange={(e: ChangeEvent<HTMLSelectElement>) => {
            resetPaginering(setPage);
            setSokefilter({ ...sokefilter, enhet: e.currentTarget.value });
          }}
        >
          <option value="">Alle enheter</option>
          {enheter?.map((enhet) => (
            <option key={enhet.enhetId} value={enhet.enhetNr}>
              {enhet.navn} - {enhet.enhetNr}
            </option>
          ))}
        </Select>
        <Select
          label="Filtrer på tiltakstype"
          hideLabel
          size="small"
          value={sokefilter.tiltakstype}
          data-testid="filter_tiltaksgjennomforing_tiltakstype"
          onChange={(e: ChangeEvent<HTMLSelectElement>) => {
            resetPaginering(setPage);
            setSokefilter({
              ...sokefilter,
              tiltakstype: e.currentTarget.value,
            });
          }}
        >
          <option value="">Alle tiltakstyper</option>
          {tiltakstyper?.data?.map((tiltakstype) => (
            <option key={tiltakstype.id} value={tiltakstype.id}>
              {tiltakstype.navn}
            </option>
          ))}
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
              sortering: e.currentTarget
                .value as SorteringTiltaksgjennomforinger,
            });
          }}
        >
          <option value="navn-ascending">Navn A-Å</option>
          <option value="navn-descending">Navn Å-A</option>
        </Select>
      </div>
    </form>
  );
}
