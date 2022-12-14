import { useQuery } from "@tanstack/react-query";
import { Ansatt } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useHentAnsatt() {
  return useQuery<Ansatt, Error>(QueryKeys.ansatt, () =>
    mulighetsrommetClient.ansatt.hentInfoOmAnsatt()
  );
}
