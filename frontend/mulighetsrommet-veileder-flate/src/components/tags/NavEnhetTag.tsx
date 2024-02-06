import { Tag } from "@navikt/ds-react";
import {
  useArbeidsmarkedstiltakFilter,
  useArbeidsmarkedstiltakFilterValue,
  valgteEnhetsnumre,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { useNavEnheter } from "@/core/api/queries/useNavEnheter";
import { NavEnhet } from "mulighetsrommet-api-client";
import Ikonknapp from "@/components/knapper/Ikonknapp";
import { XMarkIcon } from "@navikt/aksel-icons";
import styles from "./Filtertag.module.scss";

interface Props {
  handleClick?: (e: React.MouseEvent) => void;
}

export function NavEnhetTag({ handleClick }: Props) {
  const filter = useArbeidsmarkedstiltakFilterValue();
  const { data: alleEnheter } = useNavEnheter();
  const enheter = valgteEnhetsnumre(filter);
  const [, setFilter] = useArbeidsmarkedstiltakFilter();

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

      {handleClick ? (
        <Ikonknapp
          className={styles.overstyrt_ikon_knapp}
          handleClick={() => setFilter({ ...filter, regionMap: {} })}
          ariaLabel="Lukke"
          icon={<XMarkIcon className={styles.ikon} aria-label="Lukke" />}
        />
      ) : null}
    </Tag>
  );
}
