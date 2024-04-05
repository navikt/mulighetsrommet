import { Tag } from "@navikt/ds-react";
import Ikonknapp from "../../ikonknapp/Ikonknapp";
import { XMarkIcon } from "@navikt/aksel-icons";
import styles from "./Filtertag.module.scss";
import { MouseEvent } from "react";
import { NavEnhet } from "mulighetsrommet-api-client";

interface Props {
  navEnheter: NavEnhet[];
  onClose?: (e: MouseEvent) => void;
}

export function NavEnhetFiltertag({ navEnheter, onClose }: Props) {
  if (navEnheter.length === 0) {
    return null;
  }

  function tagLabel() {
    const firstEnhetName = navEnheter[0].navn;
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
      title={navEnheter.map((enhet) => enhet.navn).join(", ")}
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
