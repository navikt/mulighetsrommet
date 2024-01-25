import { Tag } from "@navikt/ds-react";
import {
  useArbeidsmarkedstiltakFilterValue,
  valgteNavEnheter,
} from "../../hooks/useArbeidsmarkedstiltakFilter";

export function NavEnhetTag() {
  const filter = useArbeidsmarkedstiltakFilterValue();
  const enheter = valgteNavEnheter(filter);

  if (!filter || enheter.length === 0) {
    return null;
  }

  function tagLabel() {
    if (enheter.length > 1) {
      return `${enheter[0].navn} +${enheter.length - 1}`;
    }
    return enheter[0].navn;
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
