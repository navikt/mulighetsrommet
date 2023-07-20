import styles from "./Notater.module.scss";
import { Alert } from "@navikt/ds-react";
import { AvtaleNotat } from "mulighetsrommet-api-client";
import SletteNotatModal from "./SletteNotatModal";
import { useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Notatkort } from "./Notatkort";

interface Props {
  notatkortListe: AvtaleNotat[];
}
export default function Notatkortliste({ notatkortListe }: Props) {
  const { data: avtale } = useAvtale();
  const [slettModal, setSlettModal] = useState(false);

  const handleSlett = () => setSlettModal(true);
  const lukkSlettModal = () => setSlettModal(false);

  return (
    <div className={styles.notatkortliste}>
      {notatkortListe === undefined || notatkortListe.length === 0 ? (
        <Alert variant="info">Det finnes ingen notater.</Alert>
      ) : (
        notatkortListe!.map((notatkort: any, index: number) => {
          return (
            <Notatkort
              notatkort={notatkort}
              handleSlett={handleSlett}
              key={index}
            />
          );
        })
      )}
      <SletteNotatModal
        modalOpen={slettModal}
        onClose={lukkSlettModal}
        avtale={avtale}
      />
    </div>
  );
}
