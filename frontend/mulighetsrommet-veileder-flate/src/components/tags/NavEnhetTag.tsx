import { Tag } from "@navikt/ds-react";
import {
  useArbeidsmarkedstiltakFilterValue,
  valgteEnhetsnumre,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { useNavEnheter } from "@/core/api/queries/useNavEnheter";
import { NavEnhet } from "mulighetsrommet-api-client";
import Ikonknapp from "@/components/knapper/Ikonknapp";
import { XMarkIcon } from "@navikt/aksel-icons";
import styles from "./Filtertag.module.scss";

interface Props {
  onClose?: (e: React.MouseEvent) => void;
}

export function NavEnhetTag({ onClose }: Props) {
  const filter = useArbeidsmarkedstiltakFilterValue();
  const { data: alleEnheter } = useNavEnheter();
  const enheter = valgteEnhetsnumre(filter);

  if (!alleEnheter || !filter || enheter.length === 0) {
    return null;
  }

  function tagLabel() {
    const firstEnhetName = alleEnheter?.find(
      (enhet: NavEnhet) => enhet.enhetsnummer === enheter[0],
    )?.navn;
    if (enheter.length > 1) {
      return `${firstEnhetName} +${enheter.length - 1}`;
    }
    return firstEnhetName;
  }

  return (
    <Tag
      key="navenhet"
      size="small"
      data-testid="filtertag_navenhet"
      title={enheter
        .map((enhet) => alleEnheter.find((e) => e.enhetsnummer === enhet)?.navn)
        .join(", ")}
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
