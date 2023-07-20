import styles from "./Notater.module.scss";
import { Alert } from "@navikt/ds-react";
import { AvtaleNotat } from "mulighetsrommet-api-client";
import SletteNotatModal from "./SletteNotatModal";
import { useState } from "react";
import { Notat } from "./Notat";

interface Props {
  notatListe: AvtaleNotat[];
}
export default function Notatliste({ notatListe }: Props) {
  const [notatIdForSletting, setNotatIdForSletting] = useState<null | string>(
    null,
  );

  return (
    <div className={styles.notatkortliste}>
      {notatListe === undefined || notatListe.length === 0 ? (
        <Alert variant="info">Det finnes ingen notater.</Alert>
      ) : (
        notatListe!.map((notatkort) => {
          return (
            <Notat
              notat={notatkort}
              handleSlett={(id: string) => setNotatIdForSletting(id)}
              key={notatkort.id}
            />
          );
        })
      )}
      <SletteNotatModal
        modalOpen={!!notatIdForSletting}
        onClose={() => setNotatIdForSletting(null)}
        notatIdForSletting={notatIdForSletting}
      />
    </div>
  );
}
