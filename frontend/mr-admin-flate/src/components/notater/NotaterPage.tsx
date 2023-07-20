import styles from "./Notater.module.scss";
import { Button, Checkbox, Heading, Loader, Textarea } from "@navikt/ds-react";
import Notatkortliste from "./Notatkortliste";
import { useAvtalenotater } from "../../api/avtaler/avtalenotat/useAvtalenotater";
import { useMineAvtalenotater } from "../../api/avtaler/avtalenotat/useMineAvtalenotater";
import { useState } from "react";
import { useLagreAvtalenotat } from "../../api/avtaler/avtalenotat/useLagreAvtalenotat";

export default function NotaterPage() {
  const { data: notatkortListe } = useAvtalenotater();
  const { data: mineNotaterListe } = useMineAvtalenotater();
  const { data: lagreAvtalenotat, isLoading: lagreAvtalenotatLoading } =
    useLagreAvtalenotat();
  const [mineNotater, setMineNotater] = useState(false);

  const liste = mineNotater ? mineNotaterListe : notatkortListe;

  const handleSubmit = () => {
    return lagreAvtalenotat;
  };

  return (
    <div className={styles.notater}>
      <form onSubmit={handleSubmit}>
        <div className={styles.notater_opprett}>
          <Textarea label={""} hideLabel className={styles.notater_input} />
          <span className={styles.notater_knapp}>
            <Button type="submit">
              {lagreAvtalenotatLoading ? <Loader /> : "Legg til notat"}
            </Button>
          </span>
        </div>
      </form>

      <div className={styles.notater_notatvegg}>
        <Heading size="medium" level="3" className={styles.notater_heading}>
          Notater
        </Heading>

        <div className={styles.notater_andrerad}>
          <Checkbox onChange={() => setMineNotater(!mineNotater)}>
            Vis kun mine notater
          </Checkbox>
        </div>

        <Notatkortliste notatkortListe={liste!} />
      </div>
    </div>
  );
}
