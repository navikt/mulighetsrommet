import styles from "./Notater.module.scss";
import { BodyLong, BodyShort, Button } from "@navikt/ds-react";
import { formaterDatoTid } from "../../utils/Utils";
import { TrashIcon } from "@navikt/aksel-icons";
import { Lenke } from "../../../../frontend-common/components/lenke/Lenke";
import { AvtaleNotat, TiltaksgjennomforingNotat } from "mulighetsrommet-api-client";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { NOM_ANSATT_SIDE } from "mulighetsrommet-frontend-common/constants";

interface Props {
  notat: AvtaleNotat | TiltaksgjennomforingNotat;
  handleSlett: (id: string) => void;
}

export function Notat({ handleSlett, notat }: Props) {
  const { data: bruker } = useHentAnsatt();

  const lagtTilAv = (notat: AvtaleNotat | TiltaksgjennomforingNotat) => {
    return (
      <div className={styles.notatinformasjon_bruker}>
        <BodyShort>Lagt til av: </BodyShort>
        <Lenke
          to={`${NOM_ANSATT_SIDE}${notat.opprettetAv.navIdent}`}
          target={"_blank"}
          className={styles.notatinformasjon_navn}
        >
          {notat.opprettetAv.navn}
        </Lenke>
        {notat.opprettetAv.navIdent === bruker!.navIdent ? (
          <Button
            variant="tertiary"
            onClick={() => handleSlett(notat.id)}
            className={styles.slette_notat}
            role="button"
          >
            <TrashIcon fontSize="1.5rem" aria-label="Slett notat" />
          </Button>
        ) : null}
      </div>
    );
  };

  return (
    <div className={styles.notat}>
      <span className={styles.notatinformasjon}>
        {lagtTilAv(notat)}
        <BodyShort>{formaterDatoTid(notat.createdAt)}</BodyShort>
      </span>
      <BodyLong>{notat.innhold}</BodyLong>
    </div>
  );
}
