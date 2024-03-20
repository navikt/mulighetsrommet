import { Tag } from "@navikt/ds-react";
import { useArbeidsmarkedstiltakFilterValue } from "@/hooks/useArbeidsmarkedstiltakFilter";
import Ikonknapp from "@/components/knapper/Ikonknapp";
import { XMarkIcon } from "@navikt/aksel-icons";
import styles from "./Filtertag.module.scss";

interface Props {
  onClose?: (e: React.MouseEvent) => void;
}

export function NavEnhetTag({ onClose }: Props) {
  const filter = useArbeidsmarkedstiltakFilterValue();

  if (!filter || filter.navEnheter.length === 0) {
    return null;
  }

  function tagLabel() {
    const firstEnhetName = filter.navEnheter[0].navn;
    if (filter.navEnheter.length > 1) {
      return `${firstEnhetName} +${filter.navEnheter.length - 1}`;
    }
    return firstEnhetName;
  }

  return (
    <Tag
      key="navenhet"
      size="small"
      data-testid="filtertag_navenhet"
      title={filter.navEnheter.map((enhet) => enhet.navn).join(", ")}
      variant="info"
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
