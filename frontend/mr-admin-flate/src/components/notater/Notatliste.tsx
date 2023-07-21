import styles from "./Notater.module.scss";
import { Alert } from "@navikt/ds-react";
import {
  AvtaleNotat,
  TiltaksgjennomforingNotat,
} from "mulighetsrommet-api-client";
import SletteNotatModal from "./SletteNotatModal";
import { useState } from "react";
import { Notat } from "./Notat";
import { UseMutationResult } from "@tanstack/react-query";

interface Props {
  notater: AvtaleNotat[] | TiltaksgjennomforingNotat[];
  visMineNotater: boolean;
  mutation: UseMutationResult<string, unknown, string>;
}
export default function Notatliste({
  notater,
  visMineNotater,
  mutation,
}: Props) {
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
          mutation={mutation}
        />
      ) : null}
    </div>
  );
}
