import { Header } from "@navikt/ds-react-internal";
import { Link } from "react-router-dom";
import { useHentAnsatt } from "../api/administrator/useHentAdministrator";

export function AdministratorHeader() {
  const response = useHentAnsatt();
  return (
    <Header>
      <Header.Title as="h1">
        <Link style={{ textDecoration: "none" }} to="/">
          NAV arbeidsmarkedstiltak
        </Link>
      </Header.Title>
      <Header.User
        data-testid="header-navident"
        name={response?.data?.ident ?? "..."}
        style={{ marginLeft: "auto" }}
      />
    </Header>
  );
}
