import styles from './FakeDoor.module.scss';
import imgUrl from '../../illustrasjonfakedoor.png';

const FakeDoor = () => {
  return (
    <div className={styles.fakedoorContainer}>
      <div className={styles.fakedoorTextContainer}>
        <h1 className={styles.fakedoorHeader}>Takk for at du deltok i piloten</h1>
        <div>
          <div>
            Piloten om arbeidsmarkedstiltak er nå avsluttet. Tusen takk for at du var med på å teste den nye tjenesten.
            Dine erfaringer og innspill er verdifulle og vil bidra til utviklingen av bedre digitale produkter i Nav.
          </div>
          <br />
        </div>
      </div>
      <div className={styles.fakedoorImage}>
        <img src={imgUrl} id="fakedoor-bilde" alt="Illustrasjon av løsning" />
      </div>
    </div>
  );
};

export default FakeDoor;
