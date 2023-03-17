import { Header } from "@navikt/ds-react-internal";
import { Link } from "react-router-dom";
import { useHentAnsatt } from "../api/administrator/useHentAdministrator";
import { capitalize } from "../utils/Utils";

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
        <Link style={{ textDecoration: "none", color: "white" }} to="/">
          NAV arbeidsmarkedstiltak
        </Link>
      </Header.Title>

      <Header.User
        data-testid="header-navident"
        name={ansattNavn}
        description={response?.data?.ident ?? "..."}
        style={{ marginLeft: "auto" }}
      />
    </Header>
  );
}
