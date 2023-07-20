import styles from "./Notater.module.scss";
import { Alert } from "@navikt/ds-react";
import { AvtaleNotat } from "mulighetsrommet-api-client";
import SletteNotatModal from "./SletteNotatModal";
import { useState } from "react";
import { Notat } from "./Notat";

interface Props {
  notater: AvtaleNotat[];
  visMineNotater: boolean;
}
export default function Notatliste({ notater, visMineNotater }: Props) {
  const [notatIdForSletting, setNotatIdForSletting] = useState<null | string>(
    null,
  );

  return (
    <div className={styles.notater}>
      {notater === undefined || notater.length === 0 ? (
        <Alert variant="info">
          {visMineNotater
            ? "Du har ingen notater."
            : "Det finnes ingen notater."}
        </Alert>
      ) : (
        notater.map((notat) => {
          return (
            <Notat
              notat={notat}
              handleSlett={(id: string) => setNotatIdForSletting(id)}
              key={notat.id}
            />
          );
        })
      )}

      {notatIdForSletting ? (
        <SletteNotatModal
          modalOpen={!!notatIdForSletting}
          onClose={() => setNotatIdForSletting(null)}
          notatIdForSletting={notatIdForSletting}
        />
      ) : null}
    </div>
  );
}
