import { Header } from "@navikt/ds-react-internal";
import { Link } from "react-router-dom";
import { useHentAnsatt } from "../api/administrator/useHentAdministrator";
import { capitalize } from "../utils/Utils";

export function AdministratorHeader() {
  const response = useHentAnsatt();
  return (
    <Header>
      <Header.Title as="p">
        <Link style={{ textDecoration: "none", color: "white" }} to="/">
          NAV arbeidsmarkedstiltak
        </Link>
      </Header.Title>
      <Header.User
        data-testid="header-navident"
        name={`${capitalize(response?.data?.fornavn)} ${capitalize(
          response?.data?.etternavn
        )}`}
        description={response?.data?.ident ?? "..."}
        style={{ marginLeft: "auto" }}
      />
    </Header>
  );
}
