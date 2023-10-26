import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { Link, useLocation } from "react-router-dom";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";

interface Props {
  onClick?: () => void;
}

export function Tilbakelenke({ onClick }: Props) {
  const { pathname } = useLocation();
  const avtaleId = useGetAvtaleIdFromUrl();
  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();

  return (
    <Link
      to={parentPath(pathname, avtaleId, tiltaksgjennomforingId)}
      data-testid="tilbakelenke"
      onClick={() => onClick?.()}
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
  } else if (pathname.includes("tiltaksgjennomforinger")) {
    if (avtaleId) {
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
