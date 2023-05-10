import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useSokBrregEnheter(sokestreng: string) {
  const enheter = [
    { organisasjonsnummer: "123456789", navn: "Testbedrift AS" },
    { organisasjonsnummer: "314587654", navn: "Ikea AS" },
  ].filter((e) => {
    return e.navn.toLowerCase().includes(sokestreng.toLowerCase());
  });
  return useQuery(QueryKeys.brregSok(sokestreng), () => enheter, {
    enabled: !!sokestreng,
  });
}
