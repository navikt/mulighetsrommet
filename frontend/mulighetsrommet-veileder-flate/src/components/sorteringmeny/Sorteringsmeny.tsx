import { Select } from '@navikt/ds-react';
import styles from './Sorteringsmeny.module.scss';

interface Props {
  sortValue: string;
  setSortValue: (a: string) => void;
}

export const Sorteringsmeny = ({ sortValue, setSortValue }: Props) => {
  return (
    <Select
      className={styles.sorteringsmeny}
      value={sortValue}
      onChange={change => setSortValue(change.currentTarget.value)}
      size="small"
      label="Hvilket felt ønsker du å sortere listen på?"
      hideLabel
      data-testid="sortering-select"
      id="sortering-select"
    >
      <option value="tiltakstypeNavn-ascending">Sorter etter:</option>
      <option value="lokasjon-ascending">Lokasjon a-å</option>
      <option value="lokasjon-descending">Lokasjon å-a</option>
      <option value="oppstart-ascending">Oppstartsdato</option>
      <option value="tiltaksgjennomforingNavn-ascending">Tittel a-å</option>
      <option value="tiltaksgjennomforingNavn-descending">Tittel å-a</option>
    </Select>
  );
};
