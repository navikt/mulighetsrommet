import { Header } from "@navikt/ds-react-internal";
import { Link } from "react-router-dom";
import { useHentAnsatt } from "../../api/administrator/useHentAdministrator";
import { NavigeringHeader } from "../../pages/forside/NavigeringHeader";
import { capitalize } from "../../utils/Utils";
import styles from "./AdministratorHeader.module.scss";

export function AdministratorHeader() {
  const response = useHentAnsatt();

  const ansattNavn = response.data?.fornavn
    ? [response.data.fornavn, response.data?.etternavn ?? ""]
        .map((it) => capitalize(it))
        .join(" ")
    : "Team Valp";

  return (
    <Header>
      <Header.Title as="h1">
        <Link className={styles.link} to="/">
          NAV arbeidsmarkedstiltak
        </Link>
      </Header.Title>
      <NavigeringHeader />
      <Header.User
        data-testid="header-navident"
        name={ansattNavn}
        description={response?.data?.ident ?? "..."}
        className={styles.user}
      />
    </Header>
  );
}
