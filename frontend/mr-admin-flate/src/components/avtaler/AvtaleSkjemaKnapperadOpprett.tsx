import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";
import { useNavigate } from "react-router-dom";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useState } from "react";
import SletteModal from "../modal/SletteModal";

interface Props {
  onClose: () => void;
  onLagreUtkast: () => void;
  error: () => void;
}
export function AvtaleSkjemaKnapperadOpprett({
  onLagreUtkast,
  error,
  onClose,
}: Props) {
  const navigate = useNavigate();
  const mutationUtkast = useMutateUtkast();
  const [sletteModal, setSletteModal] = useState(false);

  console.log(mutationUtkast);
  const handleLagreUtkast = () => {
    if (mutationUtkast.error === 0) {
      onLagreUtkast();
      navigate(`/avtaler#avtaleOversiktTab="utkast"`);
    } else {
      console.log("ERROR");

      error();
    }
  };

  return (
    <div className={styles.button_row}>
      <Button
        variant="danger"
        type="button"
        onClick={() => setSletteModal(true)}
      >
        Slett utkast
      </Button>
      <div>
        <Button
          className={styles.button}
          type="button"
          onClick={() => {
            handleLagreUtkast();
            faro?.api?.pushEvent(
              "Bruker lagrer avtaleutkast",
              { handling: "lagrer" },
              "avtale",
            );
          }}
          variant="secondary"
          data-testid="avtaleskjema-lagre-utkast"
        >
          Lagre som utkast
        </Button>

        <Button
          className={styles.button}
          type="submit"
          onClick={() => {
            faro?.api?.pushEvent(
              "Bruker oppretter avtale",
              { handling: "oppretter" },
              "avtale",
            );
          }}
        >
          Opprett avtalen
        </Button>
      </div>
      <SletteModal
        modalOpen={sletteModal}
        onClose={() => setSletteModal(false)}
        headerText="Ønsker du å slette utkastet?"
        headerTextError="Kan ikke slette notatet."
        handleDelete={onClose}
      />
    </div>
  );
}
