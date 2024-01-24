import { Tag } from "@navikt/ds-react";
import {
  navEnheter,
  useArbeidsmarkedstiltakFilterValue,
} from "../../hooks/useArbeidsmarkedstiltakFilter";
import { useNavEnheter } from "../../core/api/queries/useNavEnheter";

export function NavEnhetTag() {
  const { data: alleEnheter } = useNavEnheter();
  const filter = useArbeidsmarkedstiltakFilterValue();
  const valgteEnheter = navEnheter(filter);

  if (!alleEnheter || !valgteEnheter || !filter || valgteEnheter.length === 0) {
    return null;
  }

  function tagLabel() {
    const firstEnhetName = alleEnheter?.find((e) => e.enhetsnummer === valgteEnheter[0])?.navn;

    if (valgteEnheter.length > 1) {
      return `${firstEnhetName} +${valgteEnheter.length - 1}`;
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
