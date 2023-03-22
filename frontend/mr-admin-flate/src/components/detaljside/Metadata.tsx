import { ReactNode } from "react";
import styles from "./Metadata.module.scss";
import { BodyShort, Heading } from "@navikt/ds-react";

export function Grid({
  children,
  as,
}: {
  children: ReactNode;
  as: React.ElementType;
}) {
  const As = as;
  return <As className={styles.grid}>{children}</As>;
}

export function Metadata({
  header,
  verdi = "",
}: {
  header: string;
  verdi: string | number | undefined | null | ReactNode;
}) {
  return (
    <div className={styles.header_og_verdi}>
      <Heading level="3" size="xsmall">
        {header}
      </Heading>
      <BodyShort>{verdi}</BodyShort>
    </div>
  );
}

export function Separator() {
  return <hr className={styles.separator} />;
}
