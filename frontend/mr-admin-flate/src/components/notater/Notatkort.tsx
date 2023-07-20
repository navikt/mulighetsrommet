import styles from "./Notater.module.scss";
import { BodyLong, BodyShort, Button } from "@navikt/ds-react";
import { formaterDatoTid } from "../../utils/Utils";
import { TrashIcon } from "@navikt/aksel-icons";
import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import { AvtaleNotat } from "mulighetsrommet-api-client";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";

interface NotatkortProps {
  notatkort: AvtaleNotat;
  handleSlett: () => void;
}

export function Notatkort({ handleSlett, notatkort }: NotatkortProps) {
  const { data: bruker } = useHentAnsatt();

  const innloggetBruker = (notatkort: AvtaleNotat) => {
    return (
      <div className={styles.notatinformasjon_bruker}>
        <BodyShort>Lagt til av: </BodyShort>
        {notatkort.opprettetAv.navIdent === bruker!.navIdent ? (
          <span className={styles.notatinformasjon_egen_bruker}>
            <BodyShort className={styles.notatinformasjon_navn}>
              {notatkort.opprettetAv.navn}
            </BodyShort>
            <Button variant="tertiary" onClick={handleSlett}>
              <TrashIcon fontSize={"1.5rem"} />
            </Button>
          </span>
        ) : (
          <Lenke
            to={`https://nom.nav.no/ressurs/${notatkort.opprettetAv.navIdent}`}
            target={"_blank"}
            className={styles.notatinformasjon_navn}
          >
            {notatkort.opprettetAv.navn}
          </Lenke>
        )}
      </div>
    );
  };

  return (
    <div className={styles.notatkort}>
      <span className={styles.notatinformasjon}>
        {innloggetBruker(notatkort)}
        <BodyShort>{formaterDatoTid(notatkort.createdAt)}</BodyShort>
      </span>
      <BodyLong>{notatkort.innhold}</BodyLong>
    </div>
  );
}
