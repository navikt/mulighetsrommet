import { InternalHeader } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { NavigeringHeader } from "../../pages/forside/NavigeringHeader";
import { capitalize } from "../../utils/Utils";
import styles from "./AdministratorHeader.module.scss";
import { Notifikasjonsbjelle } from "../notifikasjoner/Notifikasjonsbjelle";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";

export function AdministratorHeader() {
  const { data } = useHentAnsatt();

  const ansattNavn = data
    ? [data.fornavn, data.etternavn].map((it) => capitalize(it)).join(" ")
    : "Team Valp";

  return (
    <InternalHeader>
      <InternalHeader.Title className={styles.title} as="h1">
        <Link className={styles.link} to="/">
          NAV arbeidsmarkedstiltak
        </Link>
      </InternalHeader.Title>
      <div className={styles.content}>
        <NavigeringHeader />
        <Notifikasjonsbjelle />
      </div>
      <InternalHeader.User
        data-testid="header-navIdent"
        name={ansattNavn}
        description={data?.navIdent ?? "..."}
        className={styles.user}
      />
    </InternalHeader>
  );
}
