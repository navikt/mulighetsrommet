import {
  FileCheckmarkIcon,
  HandshakeIcon,
  PersonPencilIcon,
  RectangleSectionsIcon,
  TokenIcon,
} from "@navikt/aksel-icons";
import { Forsidekort } from "./Forsidekort";
import styles from "./Forsidekort.module.scss";
import {
  ENDRINGSMELDINGER_URL,
  PREVIEW_ARBEIDSMARKEDSTILTAK_URL,
  SANITY_STUDIO_URL,
} from "../../constants";

export function ForsidekortListe() {
  return (
    <div className={styles.card_container}>
      <Forsidekort
        navn="Tiltakstyper"
        ikon={<TokenIcon aria-label="Tiltakstyper" />}
        url="tiltakstyper"
        tekst="Her finner du informasjon om tiltakstyper"
      />
      <Forsidekort
        navn="Avtaler"
        ikon={<HandshakeIcon aria-label="Avtaler" />}
        url="avtaler"
        tekst="Her finner du informasjon om avtaler"
      />
      <Forsidekort
        navn="Tiltaksgjennomføringer"
        ikon={<FileCheckmarkIcon aria-label="Tiltaksgjennomføringer for gruppetiltak" />}
        url="tiltaksgjennomforinger"
        tekst="Her finner du informasjon om tiltaksgjennomføringer for gruppetiltak"
      />
      <Forsidekort
        navn="Individuelle tiltaksgjennomføringer"
        ikon={<img src="./sanity_logo.png" alt="Sanity-logo" />}
        url={SANITY_STUDIO_URL}
        tekst="Her administrerer du individuelle tiltaksgjennomføringer"
      />
      <Forsidekort
        navn="Veilederflate forhåndsvisning"
        ikon={<RectangleSectionsIcon aria-label="Preview" />}
        url={PREVIEW_ARBEIDSMARKEDSTILTAK_URL}
        tekst="Her kan du se hvordan tiltakene vises for veileder i Modia"
      />
      <Forsidekort
        navn="Endringsmeldinger"
        ikon={<PersonPencilIcon aria-label="Endringsmeldinger" />}
        url={ENDRINGSMELDINGER_URL}
        tekst="Her finner du endringsmeldinger fra tiltaksarrangør"
      />
    </div>
  );
}
