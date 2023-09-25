import { Link, useLocation } from "react-router-dom";
import styles from "./Tilbakelenke.module.scss";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";
import { ChevronLeftIcon } from "@navikt/aksel-icons";

export function Tilbakelenke() {
  const { pathname } = useLocation();
  const avtaleId = useGetAvtaleIdFromUrl();
  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();

  return (
    <Link
      className={styles.tilbakelenke}
      to={parentPath(pathname, avtaleId, tiltaksgjennomforingId)}
      data-testid="tilbakelenke"
    >
      <ChevronLeftIcon aria-label="Tilbakeknapp" />
      Tilbake
    </Link>
  );
}

export const parentPath = (
  pathname: string,
  avtaleId?: string,
  tiltaksgjennomforingId?: string,
) => {
  if (pathname.includes("avtaler")) {
    if (tiltaksgjennomforingId) {
      return `/avtaler/${avtaleId}`;
    } else {
      return "/avtaler";
    }
  } else if (tiltaksgjennomforingId) {
    return "/tiltaksgjennomforinger";
  } else {
    return "/tiltakstyper";
  }
};
