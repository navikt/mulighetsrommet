import styles from "./Notater.module.scss";
import { Alert } from "@navikt/ds-react";
import { ApiError, AvtaleNotat, TiltaksgjennomforingNotat } from "mulighetsrommet-api-client";
import { useState } from "react";
import { Notat } from "./Notat";
import { UseMutationResult } from "@tanstack/react-query";
import SletteModal from "../modal/SletteModal";

interface Props {
  notater: AvtaleNotat[] | TiltaksgjennomforingNotat[];
  visMineNotater: boolean;
  mutation: UseMutationResult<string, ApiError, string>;
}

export default function Notatliste({ notater, visMineNotater, mutation }: Props) {
  const [notatIdForSletting, setNotatIdForSletting] = useState<null | string>(null);

  return (
    <div className={styles.notater}>
      {notater === undefined || notater.length === 0 ? (
        <Alert variant="info">
          {visMineNotater ? "Du har ingen notater." : "Det finnes ingen notater."}
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
        <SletteModal
          modalOpen={!!notatIdForSletting}
          onClose={() => setNotatIdForSletting(null)}
          mutation={mutation}
          headerText="Ønsker du å slette notatet?"
          headerTextError="Kan ikke slette notatet."
          handleDelete={() =>
            mutation.mutate(notatIdForSletting, {
              onSuccess: () => setNotatIdForSletting(null),
            })
          }
        />
      ) : null}
    </div>
  );
}
