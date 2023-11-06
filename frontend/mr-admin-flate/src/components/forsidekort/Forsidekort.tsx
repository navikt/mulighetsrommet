import styles from "./Forsidekort.module.scss";
import { Link } from "react-router-dom";
import { faro } from "@grafana/faro-web-sdk";
import {
  FileCheckmarkIcon,
  HandshakeIcon,
  RectangleSectionsIcon,
  TokenIcon,
} from "@navikt/aksel-icons";
import { BodyShort, Heading } from "@navikt/ds-react";
import { erForhandsvisningMiljo } from "../../utils/Utils";

export function Forsidekort() {
  const forsideKort: {
    navn: string;
    ikon: React.ReactNode;
    url: string;
    tekst?: string;
  }[] = [
    {
      navn: "Tiltakstyper",
      ikon: <TokenIcon />,
      url: "tiltakstyper",
      tekst: "Her finner du informasjon om tiltakstyper",
    },
    {
      navn: "Avtaler",
      ikon: <HandshakeIcon />,
      url: "avtaler",
      tekst: "Her finner du informasjon om avtaler",
    },
    {
      navn: "Tiltaksgjennomføringer",
      ikon: <FileCheckmarkIcon />,
      url: "tiltaksgjennomforinger",
      tekst: "Her finner du informasjon om tiltaksgjennomføringer for gruppetiltak",
    },
    {
      navn: "Individuelle tiltaksgjennomføringer",
      ikon: <img src="./sanity_logo.png" alt="Sanity-logo" />,
      url: "https://mulighetsrommet-sanity-studio.intern.nav.no/prod/desk",
      tekst: "Her administrerer du individuelle tiltaksgjennomføringer",
    },
    {
      navn: "Veilederflate forhåndsvisning",
      ikon: <RectangleSectionsIcon />,
      url: `https://mulighetsrommet-veileder-flate.intern.${erForhandsvisningMiljo}/preview`,
      tekst: "Her kan du se hvordan tiltakene vises for veileder i Modia.",
    },
  ];

  return (
    <div className={styles.card_container}>
      {forsideKort.map((card) => (
        <Link
          key={card.url}
          className={styles.card}
          to={card.url}
          onClick={() =>
            faro?.api?.pushEvent(
              `Bruker trykket på inngang fra forside: ${card.navn}`,
              {
                inngang: card.navn,
              },
              "forside",
            )
          }
        >
          <span className={styles.circle}>{card.ikon}</span>
          <Heading size="medium" level="3">
            {card.navn}
          </Heading>
          <BodyShort className={styles.infotekst}>{card.tekst}</BodyShort>
        </Link>
      ))}
    </div>
  );
}
