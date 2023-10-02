import classNames from "classnames";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { TiltakstypestatusTag } from "../../components/statuselementer/TiltakstypestatusTag";
import { erProdMiljo, formaterDato } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { Bolk } from "../../components/detaljside/Bolk";
import { Link } from "react-router-dom";
import { ExternalLinkIcon } from "@navikt/aksel-icons";

export function TiltakstypeInfo() {
  const { data } = useTiltakstypeById();

  if (!data) {
    return null;
  }

  const sanityTiltakstypeUrl =
    "https://mulighetsrommet-sanity-studio.intern.nav.no/" +
    (erProdMiljo ? "prod" : "test") +
    "/desk/tiltakstype;";

  const tiltakstype = data;
  return (
    <div className={classNames(styles.detaljer, styles.container)}>
      <div className={styles.bolk}>
        <Metadata header="Tiltakstype" verdi={tiltakstype.navn} />
        <Metadata header="Tiltakskode" verdi={tiltakstype.arenaKode} />
      </div>
      <Separator />
      <Metadata header="Status" verdi={<TiltakstypestatusTag tiltakstype={tiltakstype} />} />
      <Separator />
      <div className={styles.bolk}>
        <Metadata header="Startdato" verdi={formaterDato(tiltakstype.fraDato)} />
        <Metadata header="Sluttdato" verdi={formaterDato(tiltakstype.tilDato)} />
      </div>
      {tiltakstype.sanityId && (
        <>
          <Separator />
          <Bolk aria-label="Sanity-dokument">
            <Metadata
              header="Sanity dokument"
              verdi={
                <>
                  <Link target="_blank" to={sanityTiltakstypeUrl + tiltakstype.sanityId}>
                    Åpne tiltakstypen i Sanity{" "}
                    <ExternalLinkIcon title="Åpner tiltakstypen i Sanity" />
                  </Link>
                </>
              }
            />
          </Bolk>
        </>
      )}
    </div>
  );
}
