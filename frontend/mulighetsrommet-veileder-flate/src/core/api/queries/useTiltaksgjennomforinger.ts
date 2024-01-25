import { useQuery } from "@tanstack/react-query";
import { GetTiltaksgjennomforingerRequest, Innsatsgruppe } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import {
  useArbeidsmarkedstiltakFilterValue,
  valgteEnhetsnumre,
} from "../../../hooks/useArbeidsmarkedstiltakFilter";

export default function useTiltaksgjennomforinger() {
  const filter = useArbeidsmarkedstiltakFilterValue();

  const requestBody: GetTiltaksgjennomforingerRequest = {
    enheter: valgteEnhetsnumre(filter),
    innsatsgruppe: filter.innsatsgruppe?.nokkel,
    apentForInnsok: filter.apentForInnsok,
  };

  if (filter.search) {
    requestBody.search = filter.search;
  }

  if (filter.tiltakstyper.length > 0) {
    requestBody.tiltakstypeIds = filter.tiltakstyper.map(({ id }) => id);
  }

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforinger(filter),
    queryFn: () =>
      mulighetsrommetClient.veilederTiltak.getVeilederTiltaksgjennomforinger({
        requestBody,
      }),
  });
}

export function utledInnsatsgrupperFraInnsatsgruppe(innsatsgruppe: string): Innsatsgruppe[] {
  switch (innsatsgruppe) {
    case "STANDARD_INNSATS":
      return [Innsatsgruppe.STANDARD_INNSATS];
    case "SITUASJONSBESTEMT_INNSATS":
      return [Innsatsgruppe.STANDARD_INNSATS, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS];
    case "SPESIELT_TILPASSET_INNSATS":
      return [
        Innsatsgruppe.STANDARD_INNSATS,
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      ];
    case "VARIG_TILPASSET_INNSATS":
      return [
        Innsatsgruppe.STANDARD_INNSATS,
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
        Innsatsgruppe.VARIG_TILPASSET_INNSATS,
      ];
    default:
      return [];
  }
}
