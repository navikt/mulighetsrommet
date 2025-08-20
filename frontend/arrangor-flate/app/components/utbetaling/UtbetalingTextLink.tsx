import { Link } from "@navikt/ds-react";
import { ArrangorflateUtbetalingStatus } from "api-client";
import { Link as ReactRouterLink } from "react-router";
import { pathByOrgnr } from "~/utils/navigation";

interface UtbetalingTextLinkProps {
  status: ArrangorflateUtbetalingStatus;
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
    case ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING: {
      return (
        <Link
          as={ReactRouterLink}
          aria-label={`Start innsending for krav om utbetaling for ${gjennomforingNavn}`}
          to={pathByOrgnr(orgnr).innsendingsinformasjon(utbetalingId)}
        >
          Start innsending
        </Link>
      );
    }
    case ArrangorflateUtbetalingStatus.KREVER_ENDRING: {
      return (
        <Link
          as={ReactRouterLink}
          aria-label={`Se innsending for krav om utbetaling for ${gjennomforingNavn}`}
          to={pathByOrgnr(orgnr).beregning(utbetalingId)}
        >
          Se innsending
        </Link>
      );
    }
    case ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV:
    case ArrangorflateUtbetalingStatus.UTBETALT:
    case ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING: {
      return (
        <Link
          as={ReactRouterLink}
          aria-label={`Se detaljer for krav om utbetaling for ${gjennomforingNavn}`}
          to={pathByOrgnr(orgnr).detaljer(utbetalingId)}
        >
          Se detaljer
        </Link>
      );
    }
  }
}
