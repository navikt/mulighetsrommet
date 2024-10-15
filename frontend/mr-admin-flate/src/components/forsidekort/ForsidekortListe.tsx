import {
  ENDRINGSMELDINGER_URL,
  PREVIEW_ARBEIDSMARKEDSTILTAK_URL,
  SANITY_STUDIO_URL,
} from "@/constants";
import { AvtaleIkon } from "../ikoner/AvtaleIkon";
import { EndringsmeldingerIkon } from "../ikoner/EndringsmeldingerIkon";
import { ForhandsvisningIkon } from "../ikoner/ForhandsvisningIkon";
import { TiltaksgjennomforingIkon } from "../ikoner/TiltaksgjennomforingIkon";
import { TiltakstypeIkon } from "../ikoner/TiltakstypeIkon";
import { Forsidekort } from "./Forsidekort";
import styles from "./Forsidekort.module.scss";

export function ForsidekortListe() {
  return (
    <div className={styles.card_container}>
      <Forsidekort
        navn="Tiltakstyper"
        ikon={<TiltakstypeIkon inkluderBakgrunn aria-label="Tiltakstyper" />}
        url="tiltakstyper"
        tekst="Her finner du informasjon om tiltakstyper for gruppetiltak"
      />
      <Forsidekort
        navn="Avtaler"
        ikon={<AvtaleIkon inkluderBakgrunn aria-label="Avtaler" />}
        url="avtaler"
        tekst="Her finner du informasjon om avtaler for gruppetiltak"
      />
      <Forsidekort
        navn="Tiltaksgjennomføringer"
        ikon={
          <TiltaksgjennomforingIkon
            inkluderBakgrunn
            aria-label="Tiltaksgjennomføringer for gruppetiltak"
          />
        }
        url="tiltaksgjennomforinger"
        tekst="Her finner du informasjon om tiltaksgjennomføringer for gruppetiltak"
      />
      <Forsidekort
        navn="Individuelle tiltaksgjennomføringer"
        ikon={
          <img
            style={{ height: "64px", width: "64px" }}
            src="./sanity_logo.png"
            alt="Sanity-logo"
          />
        }
        url={SANITY_STUDIO_URL}
        apneINyTab
        tekst="Her administrerer du individuelle tiltaksgjennomføringer"
      />
      <Forsidekort
        navn="Veilederflate forhåndsvisning"
        ikon={<ForhandsvisningIkon inkluderBakgrunn aria-label="Preview" />}
        url={PREVIEW_ARBEIDSMARKEDSTILTAK_URL}
        tekst="Her kan du se hvordan tiltakene vises for veileder i Modia"
      />
      <Forsidekort
        navn="Endringsmeldinger"
        ikon={<EndringsmeldingerIkon inkluderBakgrunn aria-label="Endringsmeldinger" />}
        url={ENDRINGSMELDINGER_URL}
        tekst="Her finner du endringsmeldinger fra tiltaksarrangør"
      />
    </div>
  );
}
