import styles from "./Forsidekort.module.scss";
import { Forsidekort } from "./Forsidekort";
import {
  FileCheckmarkIcon,
  HandshakeIcon,
  RectangleSectionsIcon,
  TokenIcon,
} from "@navikt/aksel-icons";
import { erForhandsvisningMiljo } from "../../utils/Utils";

export function ForsidekortListe() {
  return (
    <div className={styles.card_container}>
      <Forsidekort
        navn="Tiltakstyper"
        ikon={<TokenIcon />}
        url="tiltakstyper"
        tekst="Her finner du informasjon om tiltakstyper"
      />
      <Forsidekort
        navn="Avtaler"
        ikon={<HandshakeIcon />}
        url="avtaler"
        tekst="Her finner du informasjon om avtaler"
      />
      <Forsidekort
        navn="Tiltaksgjennomføringer"
        ikon={<FileCheckmarkIcon />}
        url="tiltaksgjennomforinger"
        tekst="Her finner du informasjon om tiltaksgjennomføringer for gruppetiltak"
      />
      <Forsidekort
        navn="Individuelle tiltaksgjennomføringer"
        ikon={<img src="./sanity_logo.png" alt="Sanity-logo" />}
        url="https://mulighetsrommet-sanity-studio.intern.nav.no/prod/desk"
        tekst="Her administrerer du individuelle tiltaksgjennomføringer"
      />
      <Forsidekort
        navn="Veilederflate forhåndsvisning"
        ikon={<RectangleSectionsIcon />}
        url={`https://mulighetsrommet-veileder-flate.intern.${erForhandsvisningMiljo}/preview`}
        tekst="Her kan du se hvordan tiltakene vises for veileder i Modia"
      />
    </div>
  );
}
