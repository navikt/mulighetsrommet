import { useHentBrukerdata } from "../../core/api/queries/useHentBrukerdata";
import {
  Tiltaksgjennomforingsfiltergruppe,
  tiltaksgjennomforingsfilter,
} from "../../core/atoms/atoms";
import { BrukersEnhet } from "../brukersEnheter/BrukersEnhet";
import { ErrorTag } from "../tags/ErrorTag";
import FilterTag from "../tags/FilterTag";
import SearchFieldTag from "../tags/SearchFieldTag";
import styles from "./Filtertags.module.scss";
import { useAtom } from "jotai";

export function Filtertags() {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const brukerdata = useHentBrukerdata();

  return (
    <div className={styles.filtertags} data-testid="filtertags">
      <BrukersEnhet />
      {!brukerdata.isLoading &&
        !brukerdata.data?.innsatsgruppe &&
        !brukerdata.data?.servicegruppe && (
          <ErrorTag
            innhold="Innsatsgruppe og servicegruppe mangler"
            title="Kontroller om brukeren er under oppfølging og finnes i Arena"
            dataTestId="alert-innsatsgruppe"
          />
        )}
      {filter.innsatsgruppe && <FilterTag skjulIkon options={[filter.innsatsgruppe]} />}
      <FilterTag
        options={filter.tiltakstyper!}
        handleClick={(id: string) =>
          setFilter({
            ...filter,
            tiltakstyper: filter.tiltakstyper?.filter(
              (tiltakstype: Tiltaksgjennomforingsfiltergruppe<string>) => tiltakstype.id !== id,
            ),
          })
        }
      />
      <SearchFieldTag />
    </div>
  );
}
