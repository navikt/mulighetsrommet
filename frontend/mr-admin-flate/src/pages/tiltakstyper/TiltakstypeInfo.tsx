import { ExternalLinkIcon } from "@navikt/aksel-icons";
import classNames from "classnames";
import { Link } from "react-router-dom";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Bolk } from "../../components/detaljside/Bolk";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { erProdMiljo, formaterDato } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";

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
    <div className={classNames(styles.container)}>
      <div className={styles.detaljer}>
        <Bolk>
          <Metadata header="Tiltakstype" verdi={tiltakstype.navn} />
          <Metadata header="Tiltakskode" verdi={tiltakstype.arenaKode} />
        </Bolk>
        <Separator />
        <Bolk>
          <Metadata header="Startdato" verdi={formaterDato(tiltakstype.fraDato)} />
          <Metadata header="Sluttdato" verdi={formaterDato(tiltakstype.tilDato)} />
        </Bolk>
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
    </div>
  );
}
