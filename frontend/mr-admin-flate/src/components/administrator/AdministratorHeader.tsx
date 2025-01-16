import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import {
  ENDRINGSMELDINGER_URL,
  LOGOUT_AND_SELECT_ACCOUNT_URL,
  PREVIEW_ARBEIDSMARKEDSTILTAK_URL,
  SANITY_STUDIO_URL,
} from "@/constants";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { MenuGridIcon } from "@navikt/aksel-icons";
import { Dropdown, InternalHeader, Spacer } from "@navikt/ds-react";
import { useRef } from "react";
import { Link } from "react-router";
import { Notifikasjonsbjelle } from "../notifikasjoner/Notifikasjonsbjelle";
import styles from "./AdministratorHeader.module.scss";

export function AdministratorHeader() {
  const tiltakstyperLinkRef = useRef<HTMLAnchorElement>(null);
  const avtalerLinkRef = useRef<HTMLAnchorElement>(null);
  const gjennomforingerLinkRef = useRef<HTMLAnchorElement>(null);
  const arrangorerLinkRef = useRef<HTMLAnchorElement>(null);
  const individuelleGjennomforingerLinkRef = useRef<HTMLAnchorElement>(null);
  const veilederflateLinkRef = useRef<HTMLAnchorElement>(null);
  const notifikasjonerLinkRef = useRef<HTMLAnchorElement>(null);
  const endringsmeldingerLinkRef = useRef<HTMLAnchorElement>(null);
  const logoutLinkRef = useRef<HTMLAnchorElement>(null);

  return (
    <InternalHeader>
      <InternalHeader.Title className={styles.title} as="h1">
        <Link className={styles.link} to="/">
          Nav Tiltaksadministrasjon
        </Link>
      </InternalHeader.Title>
      <Spacer />
      <div className={styles.content}>
        <Notifikasjonsbjelle />
      </div>
      <Dropdown>
        <InternalHeader.Button as={Dropdown.Toggle}>
          <MenuGridIcon style={{ fontSize: "1.5rem" }} title="Meny" />
        </InternalHeader.Button>

        <Dropdown.Menu>
          <Dropdown.Menu.GroupedList>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => tiltakstyperLinkRef.current?.click()}
              as="span"
            >
              <Link ref={tiltakstyperLinkRef} to="/tiltakstyper" className={styles.menylenke}>
                Tiltakstyper
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => avtalerLinkRef.current?.click()}
              as="span"
            >
              <Link ref={avtalerLinkRef} to="/avtaler" className={styles.menylenke}>
                Avtaler
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => gjennomforingerLinkRef.current?.click()}
              as="span"
            >
              <Link ref={gjennomforingerLinkRef} to="/gjennomforinger" className={styles.menylenke}>
                Gjennomføringer
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => arrangorerLinkRef.current?.click()}
              as="span"
            >
              <Link ref={arrangorerLinkRef} to="/arrangorer" className={styles.menylenke}>
                Arrangører
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.Divider />
            <Dropdown.Menu.GroupedList.Item
              onClick={() => notifikasjonerLinkRef.current?.click()}
              as="span"
            >
              <Link ref={notifikasjonerLinkRef} to="/notifikasjoner" className={styles.menylenke}>
                Notifikasjoner
              </Link>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.Divider />
            <Dropdown.Menu.GroupedList.Item
              as="span"
              onClick={() => individuelleGjennomforingerLinkRef.current?.click()}
            >
              <Lenke
                to={SANITY_STUDIO_URL}
                target="_blank"
                onClick={(e) => e.stopPropagation()}
                className={styles.menylenke}
                isExternal
              >
                Individuelle tiltaksgjennomføringer
              </Lenke>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.GroupedList.Item
              onClick={() => veilederflateLinkRef.current?.click()}
              as="span"
            >
              <Lenke
                to={PREVIEW_ARBEIDSMARKEDSTILTAK_URL}
                target="_blank"
                onClick={(e) => e.stopPropagation()}
                className={styles.menylenke}
                isExternal
              >
                Veilederflate forhåndsvisning
              </Lenke>
            </Dropdown.Menu.GroupedList.Item>
            <Dropdown.Menu.Divider />
            <Dropdown.Menu.GroupedList.Item
              onClick={() => endringsmeldingerLinkRef.current?.click()}
              as="span"
            >
              <Link
                ref={endringsmeldingerLinkRef}
                to={ENDRINGSMELDINGER_URL}
                className={styles.menylenke}
              >
                Endringsmeldinger
              </Link>
            </Dropdown.Menu.GroupedList.Item>
          </Dropdown.Menu.GroupedList>
          <>
            <Dropdown.Menu.Divider />
            <Dropdown.Menu.List>
              <Dropdown.Menu.List.Item as="span" onClick={() => logoutLinkRef.current?.click()}>
                <a ref={logoutLinkRef} href={LOGOUT_AND_SELECT_ACCOUNT_URL}>
                  Logg ut
                </a>
              </Dropdown.Menu.List.Item>
            </Dropdown.Menu.List>
          </>
        </Dropdown.Menu>
        <InlineErrorBoundary>
          <Brukernavn />
        </InlineErrorBoundary>
      </Dropdown>
    </InternalHeader>
  );
}

function Brukernavn() {
  const { data, isLoading } = useHentAnsatt();

  if (!data || isLoading) {
    return null;
  }

  const ansattNavn = [data.fornavn, data.etternavn].join(" ");

  return (
    <InternalHeader.User
      name={ansattNavn}
      description={data?.navIdent ?? "..."}
      className={styles.user}
    />
  );
}
