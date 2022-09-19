import './FakeDoor.less';
import imgUrl from '../../illustrasjonfakedoor.png';

const FakeDoor = () => {
  return (
    <div className="fakedoor-container">
      <div className="fakedoor-text-container">
        <h1 className="fakedoor-header">Takk for at du deltok i piloten</h1>
        <div className="fakedoor-text">
          <div>
            Piloten om arbeidsmarkedstiltak er nå avsluttet. Tusen takk for at du var med på å teste den nye tjenesten.
            Dine erfaringer og innspill er verdifulle og vil bidra til utviklingen av bedre digitale produkter i Nav.
          </div>
          <br />
        </div>
      </div>
      <div className="fakedoor-image">
        <img src={imgUrl} id="fakedoor-bilde" alt="Illustrasjon av løsning" />
      </div>
    </div>
  );
};

export default FakeDoor;
