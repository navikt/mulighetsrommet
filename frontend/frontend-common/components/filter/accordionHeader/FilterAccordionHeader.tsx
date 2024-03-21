import styles from "./FilterAccordionHeader.module.scss";
import React from "react";

interface Props {
  tittel: string;
  antallValgteFilter?: number;
  tilleggsinformasjon?: React.ReactNode;
}
export function FilterAccordionHeader({ antallValgteFilter, tittel, tilleggsinformasjon }: Props) {
  return (
    <div className={styles.accordion_header_text}>
      <span>{tittel}</span>
      {tilleggsinformasjon ? <span>{tilleggsinformasjon}</span> : null}
      {antallValgteFilter && antallValgteFilter !== 0 ? (
        <span className={styles.antall_valgte_filter}>{antallValgteFilter}</span>
      ) : null}
    </div>
  );
}
