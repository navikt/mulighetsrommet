import { ExternalLinkIcon, MenuGridIcon } from "@navikt/aksel-icons";
import { Dropdown, InternalHeader, Spacer } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { Notifikasjonsbjelle } from "../notifikasjoner/Notifikasjonsbjelle";
import styles from "./AdministratorHeader.module.scss";
import { erForhandsvisningMiljo } from "../../utils/Utils";
import { useRef } from "react";

export function AdministratorHeader() {
  const { data } = useHentAnsatt();

  const ansattNavn = data ? [data.fornavn, data.etternavn].join(" ") : "Team Valp";

  const tiltakstyperLinkRef = useRef<HTMLAnchorElement>(null);
  const avtalerLinkRef = useRef<HTMLAnchorElement>(null);
  const gjennomforingerLinkRef = useRef<HTMLAnchorElement>(null);
  const individuelleGjennomforingerLinkRef = useRef<HTMLAnchorElement>(null);
  const veilederflateLinkRef = useRef<HTMLAnchorElement>(null);
  const notifikasjonerLinkRef = useRef<HTMLAnchorElement>(null);
  const endringsmeldingerLinkRef = useRef<HTMLAnchorElement>(null);

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
            <Dropdown.Menu.GroupedList.Item
              onClick={() => tiltakstyperLinkRef.current?.click()}
              as="span"
            >
              <Link ref={tiltakstyperLinkRef} to="/tiltakstyper">
                Tiltakstyper
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => avtalerLinkRef.current?.click()}
              as="span"
            >
              <Link ref={avtalerLinkRef} to="/avtaler">
                Avtaler
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => gjennomforingerLinkRef.current?.click()}
              as="span"
            >
              <Link ref={gjennomforingerLinkRef} to="/tiltaksgjennomforinger">
                Tiltaksgjennomføringer
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => notifikasjonerLinkRef.current?.click()}
              as="span"
            >
              <Link ref={notifikasjonerLinkRef} to="/notifikasjoner">
                Notifikasjoner
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => individuelleGjennomforingerLinkRef.current?.click()}
              as="span"
            >
              <Link
                ref={individuelleGjennomforingerLinkRef}
                to="https://mulighetsrommet-sanity-studio.intern.nav.no/prod/desk"
                target="_blank"
              >
                Individuelle tiltaksgjennomføringer <ExternalLinkIcon />
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => veilederflateLinkRef.current?.click()}
              as="span"
            >
              <Link
                ref={veilederflateLinkRef}
                to={`https://mulighetsrommet-veileder-flate.intern.${erForhandsvisningMiljo}/preview`}
                target="_blank"
              >
                Veilederflate forhåndsvisning <ExternalLinkIcon />
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => endringsmeldingerLinkRef.current?.click()}
              as="span"
            >
              <Link target="_blank" ref={endringsmeldingerLinkRef} to="/">
                Endringsmeldinger <ExternalLinkIcon />
              </Link>
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
