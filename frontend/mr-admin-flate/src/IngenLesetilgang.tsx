import { BodyShort, Heading } from "@navikt/ds-react";
import styles from "./IngenLesetilgang.module.scss";
import { PadlockLockedIcon } from "@navikt/aksel-icons";

export function IngenLesetilgang() {
  return (
    <main className={styles.main}>
      <div className={styles.content}>
        <PadlockLockedIcon fontSize={50} />
        <Heading size="medium">Ingen tilgang</Heading>
        <BodyShort spacing>Din bruker mangler tilgang til Nav Tiltaksadministrasjon</BodyShort>
        <BodyShort spacing>
          <a href="https://navno.sharepoint.com/:fl:/g/contentstorage/CSP_11960ac4-f590-409d-8872-73a98cf165b4/EawaFw1m8WNOqXUHldvs2isBUAaAZzfSGWqwJ3UKLKcvzw?e=MMEdPv&nav=cz0lMkZjb250ZW50c3RvcmFnZSUyRkNTUF8xMTk2MGFjNC1mNTkwLTQwOWQtODg3Mi03M2E5OGNmMTY1YjQmZD1iJTIxeEFxV0VaRDFuVUNJY25PcGpQRmx0RkhKaW55M1VyNUtybS0wWWZEVlN0d3J4WHhVczNFMlNLMHRRRVYxTTZ4SSZmPTAxN0RCNEwyRk1ESUxRMlpYUk1OSEtTNUlIU1hONlpXUkwmYz0lMkYmYT1Mb29wQXBwJnA9JTQwZmx1aWR4JTJGbG9vcC1wYWdlLWNvbnRhaW5lciZ4PSU3QiUyMnclMjIlM0ElMjJUMFJUVUh4dVlYWnVieTV6YUdGeVpYQnZhVzUwTG1OdmJYeGlJWGhCY1ZkRldrUXhibFZEU1dOdVQzQnFVRVpzZEVaSVNtbHVlVE5WY2pWTGNtMHRNRmxtUkZaVGRIZHllRmg0VlhNelJUSlRTekIwVVVWV01VMDJlRWw4TURFM1JFSTBUREpDU3pkVU0xRklSa3hGVGtKSVRFNUpOMHMzVFZKYVZrWldTdyUzRCUzRCUyMiUyQyUyMmklMjIlM0ElMjIxM2Y1MGZlYi0wODAwLTRhMTQtOTkxZS05YWM3MDY0NWFjZmUlMjIlN0Q%3D">
            Gå til Tilganger-siden
          </a>{" "}
          for å se hvordan du bestiller korrekte tilganger.
        </BodyShort>
      </div>
    </main>
  );
}
