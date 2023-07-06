import { Header } from "@navikt/ds-react-internal";
import { Link } from "react-router-dom";
import { NavigeringHeader } from "../../pages/forside/NavigeringHeader";
import { capitalize } from "../../utils/Utils";
import styles from "./AdministratorHeader.module.scss";
import { Notifikasjonsbjelle } from "../notifikasjoner/Notifikasjonsbjelle";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";

export function AdministratorHeader() {
  const response = useHentAnsatt();

  const ansattNavn = response.data
    ? [response.data.fornavn, response.data.etternavn]
        .map((it) => capitalize(it))
        .join(" ")
    : "Team Valp";

  return (
    <Header>
      <Header.Title className={styles.title} as="h1">
        <Link className={styles.link} to="/">
          NAV arbeidsmarkedstiltak
        </Link>
      </Header.Title>
      <div className={styles.content}>
        <NavigeringHeader />
        <Notifikasjonsbjelle />
      </div>
      <Header.User
        data-testid="header-navident"
        name={ansattNavn}
        description={response?.data?.navIdent ?? "..."}
        className={styles.user}
      />
    </Header>
  );
}
