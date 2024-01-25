import { Tag } from "@navikt/ds-react";
import {
  useArbeidsmarkedstiltakFilterValue,
  valgteEnhetsnumre,
} from "../../hooks/useArbeidsmarkedstiltakFilter";
import { useNavEnheter } from "../../core/api/queries/useNavEnheter";
import { NavEnhet } from "mulighetsrommet-api-client";

export function NavEnhetTag() {
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
      key={"navenhet"}
      size="small"
      data-testid="filtertag_navenhet"
      title="Valgt enhet"
      variant="info"
    >
      {tagLabel()}
    </Tag>
  );
}
