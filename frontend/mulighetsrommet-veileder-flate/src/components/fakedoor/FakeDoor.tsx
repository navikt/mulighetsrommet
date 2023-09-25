import styles from "./FakeDoor.module.scss";
import imgUrl from "../../illustrasjonfakedoor.png";

const FakeDoor = () => {
  return (
    <div className={styles.fakedoor_container}>
      <div className={styles.fakedoor_text_container}>
        <h1 className={styles.fakedoor_header}>Informasjonstjeneste for arbeidsmarkedstiltak</h1>
        <div>
          <div>
            Her vil du som veileder kunne finne kvalitetssikret informasjon om de tiltakene som er
            aktuelle for din bruker, slik at du kan gjøre gode vurderinger, gi effektiv veiledning
            og få brukeren raskere ut i rett arbeidsmarkedstiltak.
          </div>
          <br />
        </div>
      </div>
      <div className={styles.fakedoor_image}>
        <img src={imgUrl} id="fakedoor-bilde" alt="Illustrasjon av løsning" />
      </div>
    </div>
  );
};

export default FakeDoor;
