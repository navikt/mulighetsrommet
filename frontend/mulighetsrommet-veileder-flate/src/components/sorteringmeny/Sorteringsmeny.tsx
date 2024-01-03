import { Select } from "@navikt/ds-react";
import styles from "./Sorteringsmeny.module.scss";

interface Props {
  sortValue: string;
  setSortValue: (a: string) => void;
}

export const Sorteringsmeny = ({ sortValue, setSortValue }: Props) => {
  return (
    <Select
      className={styles.sorteringsmeny}
      value={sortValue}
      onChange={(change) => setSortValue(change.currentTarget.value)}
      size="small"
      label="Hvilket felt ønsker du å sortere listen på?"
      hideLabel
      data-testid="sortering-select"
    >
      <option value="tiltakstype-ascending">Sorter etter:</option>
      <option value="oppstart-ascending">Oppstartsdato synkende</option>
      <option value="oppstart-descending">Oppstartsdato stigende</option>
      <option value="navn-ascending">Tittel a-å</option>
      <option value="navn-descending">Tittel å-a</option>
    </Select>
  );
};
