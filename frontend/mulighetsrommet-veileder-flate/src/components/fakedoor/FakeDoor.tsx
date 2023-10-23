import styles from "./FakeDoor.module.scss";
import imgUrl from "../../illustrasjonfakedoor.png";
import { BodyShort, Heading } from "@navikt/ds-react";

const FakeDoor = () => {
  return (
    <div className={styles.fakedoor_container}>
      <div className={styles.fakedoor_text_container}>
        <Heading size="large">Informasjonstjeneste for arbeidsmarkedstiltak</Heading>
        <BodyShort>
          Her vil du som veileder kunne finne kvalitetssikret informasjon om de tiltakene som er
          aktuelle for din bruker, slik at du kan gjøre gode vurderinger, gi effektiv veiledning og
          få brukeren raskere ut i rett arbeidsmarkedstiltak.
        </BodyShort>
      </div>
      <div className={styles.fakedoor_image}>
        <img src={imgUrl} id="fakedoor-bilde" alt="Illustrasjon av løsning" />
      </div>
    </div>
  );
};

export default FakeDoor;
