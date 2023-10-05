import { Alert } from "@navikt/ds-react";
import { ApiError, Utkast } from "mulighetsrommet-api-client";
import { useMineUtkast } from "../../api/utkast/useMineUtkast";
import { Laster } from "../laster/Laster";
import { UtkastKort } from "./Utkastkort";
import styles from "../avtaler/AvtaleUtkast.module.scss";

interface Props {
  utkastType: Utkast.type;
}

export function UtkastListe({ utkastType }: Props) {
  const { data = [], isLoading, error } = useMineUtkast(utkastType);

  if (error as ApiError) {
    const apiError = error as ApiError;
    return (
      <Alert variant="error">Det var problemer ved henting av utkast. {apiError.message}</Alert>
    );
  }

  if (data.length === 0 && isLoading) {
    return <Laster tekst="Henter utkast..." />;
  }

  return (
    <div className={styles.utkast_liste_container}>
      {data.length === 0 ? <Alert variant="info">Du har ingen utkast</Alert> : null}
      <ul className={styles.liste}>
        {data?.map((utkast) => {
          return (
            <li key={utkast.id}>
              <UtkastKort utkast={utkast} />
            </li>
          );
        })}
      </ul>
    </div>
  );
}
