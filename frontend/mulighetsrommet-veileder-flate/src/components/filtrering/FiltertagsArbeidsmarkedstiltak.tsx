import { ApentForInnsok } from "mulighetsrommet-api-client";
import {
  ArbeidsmarkedstiltakFilterGruppe,
  useArbeidsmarkedstiltakFilterUtenBrukerIKontekst,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import FilterTag from "../tags/FilterTag";
import styles from "./Filtertags.module.scss";
import { NavEnhetTag } from "../tags/NavEnhetTag";

export function FiltertagsArbeidsmarkedstiltak() {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilterUtenBrukerIKontekst();

  return (
    <div className={styles.filtertags} data-testid="filtertags">
      {filter.search && (
        <FilterTag
          options={[{ id: "search", tittel: `'${filter.search}'` }]}
          handleClick={() => setFilter({ ...filter, search: "" })}
        />
      )}
      {filter.apentForInnsok !== ApentForInnsok.APENT_ELLER_STENGT && (
        <FilterTag
          options={[
            {
              id: filter.apentForInnsok,
              tittel: filter.apentForInnsok === ApentForInnsok.APENT ? "Åpent" : "Stengt",
            },
          ]}
          handleClick={() =>
            setFilter({
              ...filter,
              apentForInnsok: ApentForInnsok.APENT_ELLER_STENGT,
            })
          }
        />
      )}
      {filter.innsatsgruppe && (
        <FilterTag
          options={[filter.innsatsgruppe]}
          handleClick={() => {
            setFilter({ ...filter, innsatsgruppe: undefined });
          }}
        />
      )}
      <NavEnhetTag handleClick={(e: React.MouseEvent) => e.stopPropagation()} />
      <FilterTag
        options={filter.tiltakstyper}
        handleClick={(id: string) =>
          setFilter({
            ...filter,
            tiltakstyper: filter.tiltakstyper?.filter(
              (tiltakstype: ArbeidsmarkedstiltakFilterGruppe<string>) => tiltakstype.id !== id,
            ),
          })
        }
      />
    </div>
  );
}
