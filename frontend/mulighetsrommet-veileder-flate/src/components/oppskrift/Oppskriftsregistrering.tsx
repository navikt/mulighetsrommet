import { GuidePanel } from "@navikt/ds-react";
import styles from "./Oppskriftsregistrering.module.scss";
import { useFeatureToggle } from "../../core/api/feature-toggles";
import { Toggles } from "mulighetsrommet-api-client";
export function Registreringsoppskrift() {
  const { data: enableOppskrifter } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_ARENA_OPPSKRIFTER,
  );

  if (!enableOppskrifter) return null;

  // TODO Her skal det rendres oppskrift fra Sanity

  return (
    <div className={styles.container}>
      <aside className={styles.navigering}>
        <nav>
          <ol>
            <li>
              <a href="#steg1">Start arbeidsprosessen Vurder tiltaksbehov</a>
            </li>
            <li>
              <a href="#steg2">Oppgaven Vurder tiltaksbehov</a>
            </li>
          </ol>
        </nav>
      </aside>
      <section className={styles.oppskrifter}>
        <h3>Oppskrift for registrering av deltaker i Arena</h3>
        <div className={styles.steg}>
          <h4 id="steg1">
            1. Start arbeidsprosessen <b>Vurder tiltaksbehov</b>
          </h4>
          <img
            src="/oppskrifter/steg-1.jpg"
            alt="Skjermbildet fra Arena som viser fokus på Søk person"
          />
          <p>
            For disse tiltakene tar du utgangspunkt i den aktuelle deltakeren. Klikk på ikonet Søk
            person og legg inn fødsels- og personnummer.
          </p>
          <p>
            Start ny oppgave på personen. Velg oppgavetype <b>Vurder tiltaksbehov</b>, og klikk Ok.
          </p>
          <p>Arbeidsprosessen legger seg på oppgavelisten din.</p>
          <img
            src="/oppskrifter/steg-2.jpg"
            alt="Skjermbildet fra Arena som viser fokus på Søk person"
          />
        </div>
        <div className={styles.steg}>
          <h4 id="steg2">2. Oppgaven Vurder tiltaksbehov</h4>
          <img
            src="/oppskrifter/steg-3.jpg"
            alt="Skjermbildet fra Arena som viser fokus på Søk person"
          />
          <p>
            Her ser du oppgavetrinnene i arbeidsprosessen Vurder tiltaksbehov. Gjennomfør
            obligatorisk trinn Sjekk tiltakshistorikk, for å få en oversikt over tidligere
            gjennomførte/avbrutte tiltak.
          </p>
          <p>Klikk Ok for å komme tilbake til oppgavetrinnene</p>
          <img
            src="/oppskrifter/steg-4.jpg"
            alt="Skjermbildet fra Arena som viser fokus på Søk person"
          />
          <p>Gå til oppgavetrinnet Finn tiltak med vanlig sok, og klikk Ok.</p>
          <img
            src="/oppskrifter/steg-5.png"
            alt="Skjermbildet fra Arena som viser fokus på Søk person"
          />
          <p>
            Du kommer til dette bildet. Legg inn riktig tiltaksnummer der det står Løpenummer, og
            klikk på Søk. Oversikt over tiltakene og tiltaksnummer (=løpenummer) finner du på{" "}
            <a href="https://navno.sharepoint.com/sites/enhet-nav-ost-viken/Delte%20dokumenter/Tiltak/Tiltaksoversikten.pdf">
              Tiltaksoversikten
            </a>
            .
          </p>
          <GuidePanel>
            Tips! Før du klikker på Ok, kan du klikke på knappen Tiltaksmappe nederst til venstre.
            Der kan du blant annet se mer informasjon om tiltaket, hvor mange andre som er søkt inn
            og kontaktinformasjon til tiltaksarrangøren.
          </GuidePanel>
        </div>
      </section>
    </div>
  );
}
