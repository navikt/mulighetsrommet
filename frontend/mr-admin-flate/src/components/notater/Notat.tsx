import styles from "./Notater.module.scss";
import { BodyLong, BodyShort, Button } from "@navikt/ds-react";
import { formaterDatoTid } from "../../utils/Utils";
import { TrashIcon } from "@navikt/aksel-icons";
import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import { AvtaleNotat } from "mulighetsrommet-api-client";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";

interface NotatkortProps {
  notat: AvtaleNotat;
  handleSlett: (id: string) => void;
}

export function Notat({ handleSlett, notat }: NotatkortProps) {
  const { data: bruker } = useHentAnsatt();

  const lagtTilAv = (notat: AvtaleNotat) => {
    return (
      <div className={styles.notatinformasjon_bruker}>
        <BodyShort>Lagt til av: </BodyShort>
        {notat.opprettetAv.navIdent === bruker!.navIdent ? (
          <span className={styles.notatinformasjon_egen_bruker}>
            <BodyShort className={styles.notatinformasjon_navn}>
              {notat.opprettetAv.navn}
            </BodyShort>
            <Button variant="tertiary" onClick={() => handleSlett(notat.id)}>
              <TrashIcon fontSize={"1.5rem"} />
            </Button>
          </span>
        ) : (
          <Lenke
            to={`https://nom.nav.no/ressurs/${notat.opprettetAv.navIdent}`}
            target={"_blank"}
            className={styles.notatinformasjon_navn}
          >
            {notat.opprettetAv.navn}
          </Lenke>
        )}
      </div>
    );
  };

  return (
    <div className={styles.notatkort}>
      <span className={styles.notatinformasjon}>
        {lagtTilAv(notat)}
        <BodyShort>{formaterDatoTid(notat.createdAt)}</BodyShort>
      </span>
      <BodyLong>{notat.innhold}</BodyLong>
    </div>
  );
}
