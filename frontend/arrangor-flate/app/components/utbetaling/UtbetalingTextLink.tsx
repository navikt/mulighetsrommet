import { Link } from "@navikt/ds-react";
import { ArrFlateUtbetalingStatus } from "api-client";
import { Link as ReactRouterLink } from "react-router";
import { internalNavigation } from "~/internal-navigation";

interface UtbetalingTextLinkProps {
  status: ArrFlateUtbetalingStatus;
  gjennomforingNavn: string;
  utbetalingId: string;
  orgnr: string;
}

export function UtbetalingTextLink({
  status,
  gjennomforingNavn,
  utbetalingId,
  orgnr,
}: UtbetalingTextLinkProps) {
  switch (status) {
    case ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING: {
      return (
        <Link
          as={ReactRouterLink}
          aria-label={`Start innsending for krav om utbetaling for ${gjennomforingNavn}`}
          to={internalNavigation(orgnr).innsendingsinformasjon(utbetalingId)}
        >
          Start innsending
        </Link>
      );
    }
    case ArrFlateUtbetalingStatus.VENTER_PA_ENDRING: {
      return (
        <Link
          as={ReactRouterLink}
          aria-label={`Se innsending for krav om utbetaling for ${gjennomforingNavn}`}
          to={internalNavigation(orgnr).beregning(utbetalingId)}
        >
          Se hvorfor
        </Link>
      );
    }
    default: {
      return (
        <Link
          as={ReactRouterLink}
          aria-label={`Detaljer for krav om utbetaling for ${gjennomforingNavn}`}
          to={internalNavigation(orgnr).detaljer(utbetalingId)}
        >
          Detaljer
        </Link>
      );
    }
  }
}
