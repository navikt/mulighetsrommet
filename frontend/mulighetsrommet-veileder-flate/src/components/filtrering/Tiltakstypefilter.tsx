import { useTiltakstyper } from "@/core/api/queries/useTiltakstyper";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import CheckboxFilter from "./CheckboxFilter";
import styles from "@/components/filtrering/NavEnhetFilter.module.scss";

interface Props {
  antallValgteTiltakstyper: number;
}

export function Tiltakstypefilter({ antallValgteTiltakstyper }: Props) {
  const tiltakstyper = useTiltakstyper();
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  return (
    <CheckboxFilter
      accordionHeader={
        <div className={styles.accordion_header_text}>
          <span>Tiltakstyper</span>
          {antallValgteTiltakstyper !== 0 ? (
            <span className={styles.antall_filter}>{antallValgteTiltakstyper}</span>
          ) : null}
        </div>
      }
      accordionNavn="Tiltakstyper"
      options={filter.tiltakstyper}
      setOptions={(tiltakstyper) => setFilter({ ...filter, tiltakstyper })}
      data={
        tiltakstyper.data?.map((tiltakstype) => {
          return {
            id: tiltakstype.sanityId,
            tittel: tiltakstype.navn,
          };
        }) ?? []
      }
      isError={tiltakstyper.isError}
      sortert
    />
  );
}
