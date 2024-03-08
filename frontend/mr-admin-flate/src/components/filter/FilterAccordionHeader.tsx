import styles from "./FilterAccordionHeader.module.scss";

interface Props {
  tittel: string;
  antallValgteFilter?: number;
}
export function FilterAccordionHeader({ antallValgteFilter, tittel }: Props) {
  return (
    <div className={styles.accordion_header_text}>
      <span>{tittel}</span>
      {antallValgteFilter && antallValgteFilter !== 0 ? (
        <span className={styles.antall_valgte_filter}>{antallValgteFilter}</span>
      ) : null}
    </div>
  );
}
