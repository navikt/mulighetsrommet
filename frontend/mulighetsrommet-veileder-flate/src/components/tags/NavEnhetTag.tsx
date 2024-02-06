import { Tag } from "@navikt/ds-react";
import {
  useArbeidsmarkedstiltakFilterValue,
  valgteEnhetsnumre,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { useNavEnheter } from "@/core/api/queries/useNavEnheter";
import { NavEnhet } from "mulighetsrommet-api-client";
import Ikonknapp from "@/components/knapper/Ikonknapp";
import { XMarkIcon } from "@navikt/aksel-icons";
import { kebabCase } from "@/utils/Utils";
import styles from "./Filtertag.module.scss";

interface Props {
  handleClick?: (id: string) => void;
}

export function NavEnhetTag({ handleClick }: Props) {
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
      title="Valgt enhet"
      variant="info"
    >
      {tagLabel()}
      {handleClick ? (
        <Ikonknapp
          className={styles.overstyrt_ikon_knapp}
          handleClick={() => handleClick(filtertype.id)}
          ariaLabel="Lukke"
          data-testid={`filtertag_lukkeknapp_${kebabCase(filtertype.tittel)}`}
          icon={
            <XMarkIcon
              data-testid={`filtertag_lukkeknapp_${kebabCase(filtertype.tittel)}`}
              className={styles.ikon}
              aria-label="Lukke"
            />
          }
        />
      ) : null}
    </Tag>
  );
}
