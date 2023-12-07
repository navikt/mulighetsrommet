import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import {
  GetRelevanteTiltaksgjennomforingerForBrukerRequest,
  Innsatsgruppe,
} from "mulighetsrommet-api-client";
import { useAppContext } from "../../../hooks/useAppContext";
import { tiltaksgjennomforingsfilter } from "../../atoms/atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useHentBrukerdata } from "./useHentBrukerdata";

export default function useTiltaksgjennomforinger() {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  const brukerData = useHentBrukerdata();
  const { fnr } = useAppContext();

  const requestBody: GetRelevanteTiltaksgjennomforingerForBrukerRequest = {
    norskIdent: fnr,
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
    queryKey: QueryKeys.sanity.tiltaksgjennomforinger(brukerData.data, filter),
    queryFn: () =>
      mulighetsrommetClient.veilederTiltak.getRelevanteTiltaksgjennomforingerForBruker({
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
