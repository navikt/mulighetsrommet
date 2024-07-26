import { Select } from "@navikt/ds-react";
import styles from "./Sorteringsmeny.module.scss";
import { Sorteringer } from "./sorteringAtom";

interface Props {
  sortValue: Sorteringer;
  setSortValue: (a: Sorteringer) => void;
}

export function Sorteringsmeny({ sortValue, setSortValue }: Props) {
  return (
    <Select
      className={styles.sorteringsmeny}
      value={sortValue}
      onChange={(change) => setSortValue(change.currentTarget.value as Sorteringer)}
      size="small"
      label="Hvilket felt ønsker du å sortere listen på?"
      hideLabel
      data-testid="sortering-select"
    >
      <option value="tiltakstype-ascending">Tiltakstype A-Å</option>
      <option value="tiltakstype-descending">Tiltakstype Å-A</option>
      <option value="oppstart-descending">Oppstartsdato synkende</option>
      <option value="oppstart-ascending">Oppstartsdato stigende</option>
      <option value="navn-ascending">Tittel A-Å</option>
      <option value="navn-descending">Tittel Å-A</option>
    </Select>
  );
}
