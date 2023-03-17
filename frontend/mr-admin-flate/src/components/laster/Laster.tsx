import { BodyShort, Loader } from "@navikt/ds-react";
import styles from "./Laster.module.scss";

interface Props {
  tekst?: string;
  sentrert?: boolean;
  size?:
    | "3xlarge"
    | "2xlarge"
    | "xlarge"
    | "large"
    | "medium"
    | "small"
    | "xsmall";
}

export function Laster({ tekst, sentrert = true, ...rest }: Props) {
  if (sentrert) {
    return (
      <div className={styles.laster}>
        <Loader {...rest} />
        <BodyShort>{tekst}</BodyShort>
      </div>
    );
  }

  return (
    <div>
      <Loader {...rest} />
    </div>
  );
}
