import { useQuery } from "@tanstack/react-query";
import { NavAnsatt } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useHentAnsatt() {
  return useQuery<NavAnsatt, Error>(QueryKeys.ansatt(), () =>
    mulighetsrommetClient.ansatt.hentInfoOmAnsatt()
  );
}
