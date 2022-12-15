import { Dropdown, Header } from "@navikt/ds-react-internal";
import { Link } from "react-router-dom";
import { useHentAnsatt } from "../api/administrator/useHentAdministrator";
import { rolleAtom } from "../api/atoms";
import { capitalize } from "../utils/Utils";
import { useAtom } from "jotai";
import { hentAnsattsRolle, Rolle } from "../tilgang/tilgang";
import { useVisForMiljo } from "../hooks/useVisForMiljo";

interface Props {
  gjelderForMiljo: string[];
}

export function AdministratorHeader({ gjelderForMiljo }: Props) {
  const visForMiljo = useVisForMiljo(gjelderForMiljo);
  const response = useHentAnsatt();
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_, setRolle] = useAtom(rolleAtom);
  const tilganger = response?.data?.tilganger ?? [];
  const harUtviklerTilgang = tilganger.includes("UTVIKLER_VALP");
  const ansattNavn = response.data?.fornavn
    ? [response?.data?.fornavn, response.data?.etternavn]
        .map((it) => capitalize(it))
        .join(" ")
    : "Team Valp";

  const velgRolle = (rolle: Rolle) => {
    setRolle(rolle);
    location?.reload();
  };

  return (
    <Header>
      <Header.Title as="h1">
        <Link style={{ textDecoration: "none", color: "white" }} to="/">
          NAV arbeidsmarkedstiltak
        </Link>
      </Header.Title>

      {harUtviklerTilgang || visForMiljo ? (
        <Dropdown>
          <Header.UserButton
            data-testid="header-navident"
            name={ansattNavn}
            description={response?.data?.ident ?? "..."}
            style={{ marginLeft: "auto" }}
            as={Dropdown.Toggle}
          />
          <Dropdown.Menu>
            <Dropdown.Menu.List>
              {hentAnsattsRolle(response.data) === "UTVIKLER" || visForMiljo ? (
                <>
                  <Dropdown.Menu.List.Item
                    onClick={() => velgRolle("TILTAKSANSVARLIG")}
                  >
                    Jobb som tiltaksansvarlig
                  </Dropdown.Menu.List.Item>
                  <Dropdown.Menu.List.Item
                    onClick={() => velgRolle("FAGANSVARLIG")}
                  >
                    Jobb som fagansvarlig
                  </Dropdown.Menu.List.Item>
                </>
              ) : null}
            </Dropdown.Menu.List>
          </Dropdown.Menu>
        </Dropdown>
      ) : (
        <Header.User
          data-testid="header-navident"
          name={ansattNavn}
          description={response?.data?.ident ?? "..."}
          style={{ marginLeft: "auto" }}
        />
      )}
    </Header>
  );
}
