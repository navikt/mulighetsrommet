import { Link, useLocation, useNavigate } from "react-router-dom";
import styles from "./Tilbakelenke.module.scss";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";
import { ChevronLeftIcon } from "@navikt/aksel-icons";

export function Tilbakelenke() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const avtaleId = useGetAvtaleIdFromUrl();
  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();

  const navigateTilbake = () => {
    if (pathname.includes("avtaler")) {
      if (tiltaksgjennomforingId) {
        navigate(`/avtaler/${avtaleId}`);
      } else {
        navigate("/avtaler");
      }
    } else if (tiltaksgjennomforingId) {
      navigate("/tiltaksgjennomforinger");
    } else {
      navigate("/tiltakstyper");
    }
  }

  return (
    <Link
      className={styles.tilbakelenke}
      to="#"
      onClick={navigateTilbake}
      data-testid="tilbakelenke"
    >
      <ChevronLeftIcon aria-label="Tilbakeknapp" />
      Tilbake
    </Link>
  );
}
