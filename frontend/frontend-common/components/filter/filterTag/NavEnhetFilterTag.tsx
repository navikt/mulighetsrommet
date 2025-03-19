import { Tag } from "@navikt/ds-react";
import Ikonknapp from "../../ikonknapp/Ikonknapp";
import { XMarkIcon } from "@navikt/aksel-icons";
import styles from "./FilterTag.module.scss";
import { MouseEvent } from "react";

interface Props {
  navEnheter: string[];
  onClose?: (e: MouseEvent) => void;
}

export function NavEnhetFilterTag({ navEnheter, onClose }: Props) {
  if (navEnheter.length === 0) {
    return null;
  }

  function tagLabel() {
    const firstEnhetName = navEnheter[0];
    if (navEnheter.length > 1) {
      return `${firstEnhetName} +${navEnheter.length - 1}`;
    }
    return firstEnhetName;
  }

  return (
    <Tag
      size="small"
      variant="info"
      key="navenhet"
      data-testid="filtertag_navenhet"
      className={styles.filtertag}
      title={navEnheter.join(", ")}
    >
      {tagLabel()}
      {onClose ? (
        <Ikonknapp
          className={styles.overstyrt_ikon_knapp}
          handleClick={onClose}
          ariaLabel="Lukke"
          icon={<XMarkIcon className={styles.ikon} aria-label="Lukke" />}
        />
      ) : null}
    </Tag>
  );
}
