import { ExternalLinkIcon, MenuGridIcon } from "@navikt/aksel-icons";
import { Dropdown, InternalHeader, Spacer } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { Notifikasjonsbjelle } from "../notifikasjoner/Notifikasjonsbjelle";
import styles from "./AdministratorHeader.module.scss";
import { erForhandsvisningMiljo } from "../../utils/Utils";

export function AdministratorHeader() {
  const { data } = useHentAnsatt();

  const ansattNavn = data ? [data.fornavn, data.etternavn].join(" ") : "Team Valp";

  return (
    <InternalHeader>
      <InternalHeader.Title className={styles.title} as="h1">
        <Link className={styles.link} to="/">
          NAV arbeidsmarkedstiltak
        </Link>
      </InternalHeader.Title>
      <Spacer />
      <div className={styles.content}>
        <Notifikasjonsbjelle />
      </div>
      <Dropdown>
        <InternalHeader.Button as={Dropdown.Toggle}>
          <MenuGridIcon style={{ fontSize: "1.5rem" }} title="Systemer og oppslagsverk" />
        </InternalHeader.Button>

        <Dropdown.Menu>
          <Dropdown.Menu.GroupedList>
            <Dropdown.Menu.GroupedList.Item as="span">
              <Link to="/tiltakstyper">Tiltakstyper</Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item as="span">
              <Link to="/avtaler">Avtaler</Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item as="span">
              <Link to="/tiltaksgjennomforinger">Tiltaksgjennomføringer</Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item as="span">
              <Link
                to="https://mulighetsrommet-sanity-studio.intern.nav.no/prod/desk"
                target="_blank"
              >
                Individuelle tiltaksgjennomføringer <ExternalLinkIcon />
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item as="span">
              <Link
                to={`https://mulighetsrommet-veileder-flate.intern.${erForhandsvisningMiljo}/preview`}
                target="_blank"
              >
                Veilederflate forhåndsvisning <ExternalLinkIcon />
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item as="span">
              <Link to="/notifikasjoner">Notifikasjoner</Link>
            </Dropdown.Menu.GroupedList.Item>
          </Dropdown.Menu.GroupedList>
        </Dropdown.Menu>
      </Dropdown>
      <InternalHeader.User
        name={ansattNavn}
        description={data?.navIdent ?? "..."}
        className={styles.user}
      />
    </InternalHeader>
  );
}
